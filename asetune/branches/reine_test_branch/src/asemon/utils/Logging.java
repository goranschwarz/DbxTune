/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class Logging
{
	private static Logger _logger          = Logger.getLogger(Logging.class);

	public static String	java_version;

	public static String _propFile;
	public static Properties _props;

	//static long mainThreadId = -1;

	public static void init()
	{
		init( (String)null, (Properties)null );
	}
	public static void init(String propFile)
	{
		init((String)null, propFile);
	}
	public static void init(String prefix, String propFile)
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

		init(prefix, logProps);
	}

	public static void init(String prefix, Properties props)
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

			Enumeration e = props.keys();
			while(e.hasMoreElements())
			{
				String key = (String) e.nextElement();
				Object val = props.get(key);

				// hhhmmmmm fix ENV VARIABLE SUBSTITUTION OFR SOME KEYS
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

		// Setup HARDCODED, configuration for LOG4J, if not found in config file
		if (_props.getProperty("log4j.rootLogger") == null)
		{
			boolean noDefaultLoggerMessage = System.getProperty("Logging.print.noDefaultLoggerMessage", "true").equalsIgnoreCase("true");
			if (noDefaultLoggerMessage)
			{
				System.out.println("This application are using 'log4j' as the logging subsystem, go to 'http://logging.apache.org/log4j/' for how to configure it.");
				System.out.println("Cant find a 'log4j.rootLogger' in the configuration file. I will create a ConsoleAppender called 'console' with the default loglevel of 'INFO'.");
				if (_propFile != null)
				{
					System.out.println("To change debug level add 'log4j.logger.classNameToDebug=DEBUG' to the config file '"+_propFile+"'.");

					if (prefix != null  && !prefix.equals("") )
						System.out.println("If several applications are using the same config file. You can add the prefix '"+prefix+"' to the log4j entries. Example: "+prefix+"log4j.logger.classNameToDebug=DEBUG");
				}
				System.out.println("To turn OFF the above message. Use '-DLogging.print.noDefaultLoggerMessage=false' flag to the JRE when starting the app.");
			}

			_props.setProperty("log4j.rootLogger", "INFO, console");
//			_props.setProperty("log4j.rootLogger", "INFO, console, asemon");
//			_props.setProperty("log4j.rootLogger", "DEBUG, console");

//			if (_props.getProperty("log4j.appender.asemon") == null)
//				_props.setProperty("log4j.appender.asemon", "asemon.gui.GuiLogAppender");

			if (_props.getProperty("log4j.appender.console") == null)
				_props.setProperty("log4j.appender.console", "org.apache.log4j.ConsoleAppender");
			if (_props.getProperty("log4j.appender.console.layout") == null)
				_props.setProperty("log4j.appender.console.layout", "org.apache.log4j.PatternLayout");
			if (_props.getProperty("log4j.appender.console.layout.ConversionPattern") == null)
				_props.setProperty("log4j.appender.console.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");

			//if ( asemonProps.getProperty("log4j.logger.asemon") == null)
			//	 logProps.setProperty("log4j.logger.asemon", "INFO");

			// Set default logging level to WARNING for JFree Chart
			if (_props.getProperty("log4j.logger.org.jfree") == null)
				_props.setProperty("log4j.logger.org.jfree", "WARN");
		}

		// Now set the configuration for LOG4J
		PropertyConfigurator.configure(_props);


		// Print out the memory configuration
		_logger.debug("Total memory that can be used by this JVM is " + Runtime.getRuntime().maxMemory()   / 1024 / 1024 + " MB. This can be changed with the JVM flag -Xmx###m (where ### is number of MB)");
	}
}
