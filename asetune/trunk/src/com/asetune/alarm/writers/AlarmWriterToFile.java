package com.asetune.alarm.writers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.Version;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CmSettingsHelper.Type;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;


public class AlarmWriterToFile
extends AlarmWriterAbstract
{
	private static Logger _logger          = Logger.getLogger(AlarmWriterToFile.class);

	@Override
	public boolean isCallReRaiseEnabled()
	{
		return false;
	}

	/**
	 * Initialize the component
	 */
	@Override
	public void init(Configuration conf) 
	throws Exception
	{
		super.init(conf);

		// WRITE init message, jupp a little late, but I wanted to grab the _name
		_logger.info("Initializing the AlarmWriter component named '"+getName()+"'.");

		_activeAlarms_writeToFileName = conf.getProperty   (PROPKEY_activeFilename,   DEFAULT_activeFilename);
		_activeAlarms_msgTemplate     = conf.getProperty   (PROPKEY_toActiveTemplate, DEFAULT_toActiveTemplate);

		_alarmLog_writeToFileName     = conf.getProperty   (PROPKEY_logFilename,      DEFAULT_logFilename);
		_alarmLog_msgTemplate         = conf.getProperty   (PROPKEY_toLogTemplate,    DEFAULT_toLogTemplate);
		_alarmLog_maxFileSizeInMb     = conf.getIntProperty(PROPKEY_maxFileSizeInMb,  DEFAULT_maxFileSizeInMb);
		_alarmLog_maxBackupIndex      = conf.getIntProperty(PROPKEY_maxBackupIndex,   DEFAULT_maxBackupIndex);

		//------------------------------------------
		// Check for mandatory parameters
		//------------------------------------------
		if ( StringUtil.isNullOrBlank(_activeAlarms_writeToFileName) ) throw new Exception("The property '" + PROPKEY_activeFilename    + "' is mandatory for the AlarmWriter named '"+getName()+"'.");
		if ( StringUtil.isNullOrBlank(_activeAlarms_msgTemplate)     ) throw new Exception("The property '" + PROPKEY_toActiveTemplate  + "' is mandatory for the AlarmWriter named '"+getName()+"'.");
		
		
		//------------------------------------------
		// Check for valid configuration
		//------------------------------------------
		checkAlarmActiveFileForWrite();
		checkAlarmLogFileForWrite();
	}

	private void checkAlarmActiveFileForWrite()
	throws Exception
	{
		// Check that we can write to the ACTIVE file
		try
		{
			PrintStream writeToFile = new PrintStream( new FileOutputStream(_activeAlarms_writeToFileName) ); // LEAVE THIS TO OVERWRITE
			writeToFile.close();
		}
		catch (FileNotFoundException e) // If the directory isn't found
		{
			String msg = "The AlarmWriter named '"+getName()+"' can't open the ACTIVE writer file '"+_activeAlarms_writeToFileName+"'.";
			_logger.error(msg);
			throw new Exception(msg, e);
		}
	}

	private void checkAlarmLogFileForWrite()
	throws Exception
	{
		// Check that we can write to the LOG file
		if (StringUtil.hasValue(_alarmLog_writeToFileName))
		{
			if ( StringUtil.isNullOrBlank(_alarmLog_msgTemplate) ) 
				throw new Exception("The property '" + PROPKEY_toLogTemplate  + "' is mandatory when a LOG File is given... for the AlarmWriter named '"+getName()+"'.");

			try
			{
				// TEST to write to file...
				PrintStream writeToFile = new PrintStream( new FileOutputStream(_alarmLog_writeToFileName, true) ); // THIS SHOULD BE APPEND
				writeToFile.close();
			}
			catch (FileNotFoundException e) // If the directory isn't found
			{
				String msg = "The AlarmWriter named '"+getName()+"' can't open the LOG writer file '"+_activeAlarms_writeToFileName+"'.";
				_logger.error(msg);
				throw new Exception(msg, e);
			}
		}
	}


	private void checkForConfigChanges()
	{
		Configuration conf = getConfiguration();
		if (conf == null)
			return;

		String new_activeAlarms_writeToFileName = conf.getProperty(PROPKEY_activeFilename,   DEFAULT_activeFilename);
		String new_alarmLog_writeToFileName     = conf.getProperty(PROPKEY_logFilename,      DEFAULT_logFilename);

		if ( ! new_activeAlarms_writeToFileName.equals(_activeAlarms_writeToFileName) )
		{
			_logger.info("Alarm Writer named '"+getName()+"' detected config change for '"+PROPKEY_activeFilename+"', oldVal='"+_activeAlarms_writeToFileName+"', newVal='"+new_activeAlarms_writeToFileName+"'.");
			_activeAlarms_writeToFileName = new_activeAlarms_writeToFileName;

			// Check/Create the file, do not care about exceptions
			try { checkAlarmActiveFileForWrite(); }
			catch(Exception ignore) {}
		}

		if ( ! new_alarmLog_writeToFileName.equals(_alarmLog_writeToFileName) )
		{
			_logger.info("Alarm Writer named '"+getName()+"' detected config change for '"+PROPKEY_logFilename+"', oldVal='"+_alarmLog_writeToFileName+"', newVal='"+new_alarmLog_writeToFileName+"'.");
			_alarmLog_writeToFileName = new_alarmLog_writeToFileName;

			// Check/Create the file, do not care about exceptions
			try { checkAlarmLogFileForWrite(); }
			catch(Exception ignore) {}
		}
	}
	
	//-------------------------------------------------------
	// class members
	//-------------------------------------------------------
	private String      _activeAlarms_writeToFileName = "";
	private String      _activeAlarms_msgTemplate     = "";

	private String      _alarmLog_writeToFileName     = "";
	private String      _alarmLog_msgTemplate         = "";
	private int         _alarmLog_maxFileSizeInMb     = 10;
	private int         _alarmLog_maxBackupIndex      = 3;
	//-------------------------------------------------------

	@Override
	public void printConfig()
	{
		int spaces = 45;
		_logger.info("Configuration for Alarm Writer Module: "+getName());
		_logger.info("    " + StringUtil.left(PROPKEY_activeFilename  , spaces) + ": " + _activeAlarms_writeToFileName);
		_logger.info("    " + StringUtil.left(PROPKEY_toActiveTemplate, spaces) + ": " + _activeAlarms_msgTemplate);
		_logger.info("    " + StringUtil.left(PROPKEY_logFilename     , spaces) + ": " + _alarmLog_writeToFileName);
		_logger.info("    " + StringUtil.left(PROPKEY_toLogTemplate   , spaces) + ": " + _alarmLog_msgTemplate);
		_logger.info("    " + StringUtil.left(PROPKEY_maxFileSizeInMb , spaces) + ": " + _alarmLog_maxFileSizeInMb);
		_logger.info("    " + StringUtil.left(PROPKEY_maxBackupIndex  , spaces) + ": " + _alarmLog_maxBackupIndex);
	}

	@Override
	public List<CmSettingsHelper> getAvailableSettings()
	{
		ArrayList<CmSettingsHelper> list = new ArrayList<>();
		
		Configuration conf = Configuration.getCombinedConfiguration();

		list.add( new CmSettingsHelper("ActiveFileName",    Type.MANDATORY, PROPKEY_activeFilename,   String.class,  conf.getPropertyRaw(PROPKEY_activeFilename,   DEFAULT_activeFilename),   DEFAULT_activeFilename,   "A file where ACTIVE alarms are written to on every 'end-of-scan'. Note starting "+Version.getAppName()+" with -DKEYNAME1=xxx will enable you to use ${KEYNAME1} in the setting value. Note2: ${SERVERNAME} variable is set after a successfull DBMS Connection has been made."));
		list.add( new CmSettingsHelper("ActiveMsgTemplate", Type.MANDATORY, PROPKEY_toActiveTemplate, String.class,  conf.getPropertyRaw(PROPKEY_toActiveTemplate, DEFAULT_toActiveTemplate), DEFAULT_toActiveTemplate, "Template for what should be written to the ACTIVE filename"));
		list.add( new CmSettingsHelper("LogFileName",       Type.PROBABLY,  PROPKEY_logFilename,      String.class,  conf.getPropertyRaw(PROPKEY_logFilename,      DEFAULT_logFilename),      DEFAULT_logFilename,      "Write all alarms that is raised/canceled to this file.  Note starting "+Version.getAppName()+" with -DKEYNAME1=xxx will enable you to use ${KEYNAME1} in the setting value. Note2: ${SERVERNAME} variable is set after a successfull DBMS Connection has been made."));
		list.add( new CmSettingsHelper("LogMsgTemplate",    Type.PROBABLY,  PROPKEY_toLogTemplate,    String.class,  conf.getPropertyRaw(PROPKEY_toLogTemplate,    DEFAULT_toLogTemplate),    DEFAULT_toLogTemplate,    "Template for what should be written to the LOG filename"));
		list.add( new CmSettingsHelper("LogMaxSize",                        PROPKEY_maxFileSizeInMb,  Integer.class, conf.getIntProperty(PROPKEY_maxFileSizeInMb,  DEFAULT_maxFileSizeInMb),  DEFAULT_maxFileSizeInMb,  "How many MB can the log file be."));
		list.add( new CmSettingsHelper("LogRollover",                       PROPKEY_maxBackupIndex,   Integer.class, conf.getIntProperty(PROPKEY_maxBackupIndex,   DEFAULT_maxBackupIndex),   DEFAULT_maxBackupIndex,   "Log 'rollover' (when log file reaches #MB start a new log file). Save maximum # number of files."));

		return list;
	}

	public static final String  PROPKEY_activeFilename                = "AlarmWriterToFile.active.filename";
	public static final String  DEFAULT_activeFilename                = "${HOME}/.dbxtune/dbxc/log/ALARM.ACTIVE.${SERVERNAME:-unknown}.txt";

	public static final String  PROPKEY_toActiveTemplate              = "AlarmWriterToFile.active.msg.template";
	public static final String  DEFAULT_toActiveTemplate              = createActiveTemplate();

	public static final String  PROPKEY_logFilename                   = "AlarmWriterToFile.log.filename";
	public static final String  DEFAULT_logFilename                   = "${HOME}/.dbxtune/dbxc/log/ALARM.LOG.${SERVERNAME:-unknown}.log";

	public static final String  PROPKEY_toLogTemplate                 = "AlarmWriterToFile.log.msg.template";
	public static final String  DEFAULT_toLogTemplate                 = createLogTemplate();

	public static final String  PROPKEY_maxFileSizeInMb               = "AlarmWriterToFile.log.maxFileSizeInMb";
	public static final int     DEFAULT_maxFileSizeInMb               = 10;
	
	public static final String  PROPKEY_maxBackupIndex                = "AlarmWriterToFile.log.maxBackupIndex";
	public static final int     DEFAULT_maxBackupIndex                = 3;
	

	public static String createLogTemplate()
	{
		return "${StringUtil.format('%-23s - %-10s - %-40s - %-30s - %-30s - %-30s - %-10s - %-10s - %-10s - %8.8s - %s', ${crTimeStr}, ${type}, ${alarmClass}, ${serviceName}, ${serviceInfo}, ${extraInfo}, ${category}, ${severity}, ${state}, ${duration}, ${description})}";
	}

	public static String createActiveTemplate()
	{
		return ""
			+ "${StringUtil.format('%-30s %-30s %-30s %-30s %-10s %-10s %-10s %8.8s %s', 'AlarmClassAbriviated', 'ServerName', 'CmName', 'ExtraInfo', 'Category', 'Severity', 'State', 'Duration', 'Description')}\n"
			+ "${StringUtils.repeat('-', 30)} ${StringUtils.repeat('-', 30)} ${StringUtils.repeat('-', 30)} ${StringUtils.repeat('-', 30)} ${StringUtils.repeat('-', 10)} ${StringUtils.repeat('-', 10)} ${StringUtils.repeat('-', 10)} ${StringUtils.repeat('-', 8)} ${StringUtils.repeat('-', 80)}\n"
			+ "#foreach( $alarm in $activeAlarmList )\n"
			+ "${StringUtil.format('%-30s %-30s %-30s %-30s %-10s %-10s %-10s %8.8s %s', ${alarm.alarmClassAbriviated}, ${alarm.serviceName}, ${alarm.serviceInfo}, ${alarm.extraInfo}, ${alarm.category}, ${alarm.severity}, ${alarm.state}, ${alarm.duration}, ${alarm.description})}\n"
			+ "#end"
			;
	}
	
	
	/**
	 * A alarm has been raised by the AlarmHandler
	 */
	@Override
	public void raise(AlarmEvent alarmEvent) 
	{
		_logger.debug(getName()+": -----RAISE-----: "+alarmEvent);

		alarmLogAppend(ACTION_RAISE, alarmEvent);
	}

	/**
	 * A alarm has been re-raised by the AlarmHandler
	 */
	@Override
	public void reRaise(AlarmEvent alarmEvent) 
	{
		// EMPTY: RE-RAISE is disabled for this writer
	}

	/**
	 * A alarm has been canceled by the AlarmHandler
	 */
	@Override
	public void cancel(AlarmEvent alarmEvent) 
	{
		_logger.debug(getName()+": -----CANCEL-----: "+alarmEvent);

		alarmLogAppend(ACTION_CANCEL, alarmEvent);
	}

	/**
	 * At the end of a scan for suspected components, this method is called 
	 * and it can be used for "sending of a batch" of events or flushing a file
	 * or wahtever you want to do.
	 * Even if no alarms was raised or canceled during the "scan", this method
	 * will always be called after all components has been checked.
	 */
	@Override
	public void endOfScan(List<AlarmEvent> activeAlarms)
	{
		//System.out.println("||||"+getName()+": -----END-OF-SCAN-----.");
		_logger.debug(getName()+": -----END-OF-SCAN-----.");
		writeActiveAlarmFile(activeAlarms);
	}

	/**
	 * When the AlarmHandler initiates, it restores old alarms from privious sessions
	 * this method is called from the AlarmHandler.init() after the old alarms has been
	 * restored. 
	 * <p>
	 * @param activeAlarms a list of alarms that was restored 
	 *                     Note: do NOT remove entriws from the list, 
	 *                     it should only be read. If you want to manipulate it.
	 *                     Create your own copy of the list.
	 */
	@Override
	public void restoredAlarms(List<AlarmEvent> restoredAlarms)
	{
		_logger.debug(getName()+": -----RESTORED-ALARMS-----.");
		writeActiveAlarmFile(restoredAlarms);
	}
	
	@Override
	public String getDescription()
	{
		return "Write Active Alarms to a file. Also write Raise/Cancel request to a log file.";
	}
	
	
	/**
	 * Append the information to the "alarm log" file, if there are any...
	 * 
	 * @param activeAlarms list of the alarms.
	 */
	private void alarmLogAppend(String action, AlarmEvent alarmEvent)
	{
		checkForConfigChanges();

		if (StringUtil.isNullOrBlank(_alarmLog_writeToFileName))
			return;

		File alarmLogFile = new File(_alarmLog_writeToFileName);
		long rolloverSize = _alarmLog_maxFileSizeInMb * 1024 * 1024;
		long fileSize     = alarmLogFile.length();

		if (_logger.isDebugEnabled())
			_logger.debug("rollover="+(fileSize > rolloverSize)+": filesize='"+fileSize+"', rolloverSize='"+rolloverSize+"'. filesizeInKb='"+(fileSize/1024)+"', rolloverSizeInKb='"+(rolloverSize/1024)+"'. filesizeInMb='"+(fileSize/1024/1024)+"', rolloverSizeInMb='"+(rolloverSize/1024/1024)+"'.");

		if (fileSize > rolloverSize)
		{
			if (_alarmLog_maxBackupIndex > 0) 
			{
				// If we have a file named as the MAX backup index, delete it
				File file = new File(_alarmLog_writeToFileName + '.' + _alarmLog_maxBackupIndex);
				if (file.exists())
				{
					_logger.debug("Deleting file " + file + " to it was the last backup.");
					file.delete();
				}

				// Move files "one rollup higher": xxx.4 -> xxx.5
				File target;
				for (int i = _alarmLog_maxBackupIndex - 1; i >= 1; i--) 
				{
					file = new File(_alarmLog_writeToFileName + "." + i);
					if (file.exists()) 
					{
						target = new File(_alarmLog_writeToFileName + '.' + (i + 1));
						_logger.debug("Renaming file " + file + " to " + target);
						file.renameTo(target);
					}
				}

				// Now move the CURRENT file into backup index 1
				target = new File(_alarmLog_writeToFileName + "." + 1);

				// if the file is OPEN close it here...
				// not needed to be done, since we open it for every write

				file = new File(_alarmLog_writeToFileName);
				_logger.debug("Renaming file " + file + " to " + target);
				file.renameTo(target);
			}
		}

		// NOW OPEN THE FILE AND WRITE TO IT
		PrintStream writeToFile = null;
		try
		{
			writeToFile = new PrintStream( new FileOutputStream(_alarmLog_writeToFileName, true) ); // APPEND MODE
		}
		catch (FileNotFoundException e)
		{
			String msg = "The AlarmWriter named '"+getName()+"' can't open the writer file '"+_alarmLog_writeToFileName+"'.";
			_logger.error(msg);
			return;
		}
		
//		String logDate = _dateFormater.format( new Date(System.currentTimeMillis()) );
//
//		writeToFile.println(logDate + " - " + action + " - " + alarmEvent.toString());
//		writeToFile.close();

		// Translate template into a real string
		String writeText = WriterUtils.createMessageFromTemplate(action, alarmEvent, _alarmLog_msgTemplate, true);

		writeToFile.println(writeText);
		writeToFile.close();
	}

	
	/**
	 * Write the mapped set of alarms to the output file.
	 * 
	 * @param activeAlarms
	 *            list of the alarms.
	 */
	private void writeActiveAlarmFile(List<AlarmEvent> activeAlarms)
	{
		checkForConfigChanges();

		// Reset the file...
		PrintStream writeToFile = null;
		try
		{
			// TEST to write to file...
			writeToFile = new PrintStream( new FileOutputStream(_activeAlarms_writeToFileName) ); // OVERWRITE MODE
		}
		catch (FileNotFoundException e)
		{
			String msg = "The AlarmWriter named '"+getName()+"' cant open the writer file '"+_activeAlarms_writeToFileName+"'.";
			_logger.error(msg);
			return;
		}

		if (activeAlarms == null || (activeAlarms != null && activeAlarms.size()==0))
		{
			if (writeToFile != null)
			{
				writeToFile.println("NO ACTIVE ALARMS.");
				writeToFile.close();
			}
			return;
		}

		// Translate template into a real string
		String writeText = WriterUtils.createMessageFromTemplate(getClass().getSimpleName(), activeAlarms, _activeAlarms_msgTemplate, true);

		writeToFile.println(writeText);
		writeToFile.close();
	}
}
