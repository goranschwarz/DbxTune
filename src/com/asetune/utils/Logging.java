/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.asetune.AppDir;
import com.asetune.Version;


public class Logging
{
	private static Logger _logger          = Logger.getLogger(Logging.class);

	public static String	java_version;

	public static String _propFile;
	public static Properties _props;
	private static String _logfile = null;

	//static long mainThreadId = -1;

	public static void init()
	{
		init( (String)null, (Properties)null, null );
	}
	public static void init(String propFile)
	{
		init((String)null, propFile, null);
	}
	public static void init(String prefix, String propFile, String logFilename)
	{
		_propFile     = propFile;

		// Get properties that has to do with LOGGING
		Properties logProps = new Properties();

		if (_propFile != null)
		{
			try
			{
				FileInputStream in = new FileInputStream(_propFile);
				logProps.load(in);
				in.close();
			}
			catch (FileNotFoundException e)
			{
				System.out.println("Unable to find the file '"+_propFile+"', continuing anyway, using hard coded values for logging.");
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}

		init(prefix, logProps, logFilename);
	}

	public static void init(String prefix, Properties props, String logFilename)
	{
		if (props == null)
			props = new Properties();

		///////////////////////////////////////////////
		// Strip of the prefix of all properties
		///////////////////////////////////////////////
		if (prefix != null  && !prefix.equals("") )
		{
			Properties newProps = new Properties();

			if ( ! prefix.endsWith(".") )
				prefix = prefix + ".";

			Enumeration<Object> e = props.keys();
			while(e.hasMoreElements())
			{
				String key = (String) e.nextElement();
				Object val = props.get(key);

				// hhhmmmmm fix ENV VARIABLE SUBSTITUTION OF SOME KEYS
				if (key.matches("log4j\\.appender\\..*\\.File"))
				{
					//System.out.println("======================= BEFORE: val='"+val+"'.");
					if ( val instanceof String )
					{
						val = StringUtil.envVariableSubstitution((String)val);
					}
					//System.out.println("======================= AFTER:  val='"+val+"'.");
				}

				if (key.startsWith(prefix))
				{
					String newKey = key.replaceFirst(prefix, "");
					newProps.put(newKey, val);
				}
				else
				{
					newProps.put(key, val);
				}
			}

			props = newProps;
		}

		_props = props;
		String logfile = logFilename;

		// Setup HARDCODED, configuration for LOG4J, if not found in config file
		if (_props.getProperty("log4j.rootLogger") == null)
		{
			boolean noDefaultLoggerMessage = System.getProperty("Logging.print.noDefaultLoggerMessage", "true").equalsIgnoreCase("true");
			if (noDefaultLoggerMessage)
			{
				System.out.println("This application are using 'log4j' as the logging subsystem, go to 'http://logging.apache.org/log4j/' for how to configure it.");
				System.out.println("Can't find a 'log4j.rootLogger' in the configuration file. I will create a ConsoleAppender called 'console' with the default loglevel of 'INFO'.");
				if (_propFile != null)
				{
					System.out.println("To change debug level add 'log4j.logger.classNameToDebug=DEBUG' to the config file '"+_propFile+"'.");

					if (prefix != null  && !prefix.equals("") )
						System.out.println("If several applications are using the same config file. You can add the prefix '"+prefix+"' to the log4j entries. Example: "+prefix+"log4j.logger.classNameToDebug=DEBUG");
				}
				System.out.println("To turn OFF the above message. Use '-DLogging.print.noDefaultLoggerMessage=false' flag to the JRE when starting the app.");
			}

			boolean console = System.getProperty("Logging.console", "true").equalsIgnoreCase("true");
			if (console)
			{
				_props.setProperty("log4j.rootLogger", "INFO, console, logfile");
//				_props.setProperty("log4j.rootLogger", "INFO, console, asetune");
				
				if (System.getProperty("log.console.debug", "false").equalsIgnoreCase("true"))
					_props.setProperty("log4j.rootLogger", "DEBUG, console");
			}
			else
			{
				_props.setProperty("log4j.rootLogger", "INFO, logfile");
			}

//			if (_props.getProperty("log4j.appender.asetune") == null)
//				_props.setProperty("log4j.appender.asetune", "com.asetune.gui.GuiLogAppender");

			// Console
			if (_props.getProperty("log4j.appender.console") == null)
				_props.setProperty("log4j.appender.console", "org.apache.log4j.ConsoleAppender");
			if (_props.getProperty("log4j.appender.console.layout") == null)
				_props.setProperty("log4j.appender.console.layout", "org.apache.log4j.PatternLayout");
			if (_props.getProperty("log4j.appender.console.layout.ConversionPattern") == null)
				_props.setProperty("log4j.appender.console.layout.ConversionPattern", DEFAULT_LOG_CONSOLE_PATTERN);

			// logfile, add / or \ if it's not at the end.
			if (logfile == null)
			{
    			logfile = (AppDir.getAppStoreDir() != null) ? AppDir.getAppStoreDir() : System.getProperty("user.home");
    			if ( logfile != null && ! (logfile.endsWith("/") || logfile.endsWith("\\")) )
    				logfile += File.separatorChar;
    
				logfile += "log" + File.separatorChar;

    			if (prefix != null  && !prefix.equals("") )
    				logfile += Version.getAppName()+"."+prefix+"log";
    			else
    				logfile += Version.getAppName()+".log";
			}
			
			if (_props.getProperty("log4j.appender.logfile") == null)
				_props.setProperty("log4j.appender.logfile", "org.apache.log4j.RollingFileAppender");
			if (_props.getProperty("log4j.appender.logfile.File") == null)
				_props.setProperty("log4j.appender.logfile.File", logfile);
			if (_props.getProperty("log4j.appender.logfile.maxFileSize") == null)
				_props.setProperty("log4j.appender.logfile.maxFileSize", "10MB");
			if (_props.getProperty("log4j.appender.logfile.MaxBackupIndex") == null)
				_props.setProperty("log4j.appender.logfile.MaxBackupIndex", "3");
			if (_props.getProperty("log4j.appender.logfile.layout") == null)
				_props.setProperty("log4j.appender.logfile.layout", "org.apache.log4j.PatternLayout");
			if (_props.getProperty("log4j.appender.logfile.layout.ConversionPattern") == null)
				_props.setProperty("log4j.appender.logfile.layout.ConversionPattern", DEFAULT_LOG_FILE_PATTERN);

			//if ( asetuneProps.getProperty("log4j.logger.asetune") == null)
			//	 logProps.setProperty("log4j.logger.asetune", "INFO");

			// Set default logging level to WARNING for JFree Chart
			if (_props.getProperty("log4j.logger.org.jfree") == null)
				_props.setProperty("log4j.logger.org.jfree", "WARN");
		}

		// Now set the configuration for LOG4J
		PropertyConfigurator.configure(_props);

		// Print the Rolling log filename
		if (logfile != null)
		{
			_logfile = logfile;
			_logger.info("Logfile used for "+Version.getAppName()+" will be '"+logfile+"'.");
		}

		// Print out the memory configuration
		_logger.debug("Total memory that can be used by this JVM is " + (Runtime.getRuntime().maxMemory() / 1024 / 1024) + " MB. This can be changed with the JVM flag -Xmx###m (where ### is number of MB)");
	}
	
	public static final String DEFAULT_LOG_CONSOLE_PATTERN = "%d - %-5p - %-30t - %-30c{1} - %m%n";
	public static final String DEFAULT_LOG_FILE_PATTERN    = "%d - %-5p - %-30t - %-4L:%-30F - %m%n";

	/**
	 * Get name of FIRST FILE log file name and append the 'appendName' to the log file:
	 * "c:\xxxx\yyy.log" returns "c:\xxxx\yyy${appendName} 
	 * @param appendName
	 * @return NULL if no log append was found, else current logname + ${appendName}
	 */
	public static File getBaseLogFile(String appendName)
	{
		String logfile = _logfile;

		// if no log file check for any log files in the Log4j
		if (logfile == null)
		{
			Enumeration<Appender> e = Logger.getRootLogger().getAllAppenders();
			while ( e.hasMoreElements() )
			{
				Appender app = (Appender)e.nextElement();
				if ( app instanceof FileAppender )
				{
					logfile = ((FileAppender)app).getFile();
					if (StringUtil.hasValue(logfile))
					{
						_logger.info("Extracted log file from Appender '"+app.getName()+"', filename '"+logfile+"'.");
						break;
					}
				}
			}
		}
		
		if (StringUtil.hasValue(logfile))
		{
			File f = new File(logfile);
			String newBaseFileName = f.getName();
			if (newBaseFileName.endsWith(".log"))
				newBaseFileName = newBaseFileName.replace(".log", "");
			
			String newLogDirName = f.getAbsoluteFile().getParentFile() + "";
			String newLogfileName = newLogDirName + File.separatorChar + newBaseFileName + appendName;
			return new File(newLogfileName);
		}

		return null;
	}

}
