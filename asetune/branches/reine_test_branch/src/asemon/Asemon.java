/**
 */
package asemon;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.ToolTipManager;
import javax.swing.UIManager;

import org.apache.log4j.Logger;

import asemon.check.CheckForUpdates;
import asemon.gui.GuiLogAppender;
import asemon.gui.MainFrame;
import asemon.utils.Configuration;
import asemon.utils.Logging;

public class Asemon
{
	private static Logger _logger          = Logger.getLogger(Asemon.class);

	private boolean	     packFrame	= false;
//	public static String	java_version;

	private static Configuration asemonProps      = null;
	private static Configuration asemonSaveProps  = null;
	private static Configuration storeConfigProps = null;

	/** This can be either GUI or NO-GUI "collector" */
	private static GetCounters   getCnt          = null;

	//static long mainThreadId = -1;

	private static boolean _gui = true;

	public Asemon(String propFile, String savePropFile)
	throws Exception
	{
		// The SAVE Properties...
		asemonSaveProps = new Configuration(savePropFile);
		Configuration.setInstance(Configuration.TEMP, asemonSaveProps);

		// Get the "OTHER" properties that has to do with LOGGING etc...
		asemonProps = new Configuration(propFile);
		Configuration.setInstance(Configuration.CONF, asemonProps);

		String storeConfigFile = System.getProperty("asemon.store.config");
		// The Store Properties...
		storeConfigProps = new Configuration(storeConfigFile);
		Configuration.setInstance(Configuration.PCS, storeConfigProps);

		// Check if we are in NO-GUI mode
		_gui = System.getProperty("asemon.gui", "true").trim().equalsIgnoreCase("true");

		
		// Setup HARDCODED, configuration for LOG4J, if not found in config file
		if (_gui)
			Logging.init(null, propFile);
		else
			Logging.init("nogui.", propFile);

		
		// Check for System/localStored proxy settings
		String httpProxyHost = System.getProperty("http.proxyHost");
		String httpProxyPort = System.getProperty("http.proxyPort");
		
		if (httpProxyHost == null)
			httpProxyHost = asemonSaveProps.getProperty("http.proxyHost");
		if (httpProxyPort == null)
			httpProxyPort = asemonSaveProps.getProperty("http.proxyPort");

		if (httpProxyHost != null)
			System.setProperty("http.proxyHost", httpProxyHost);
		if (httpProxyPort != null)
			System.setProperty("http.proxyPort", httpProxyPort);

		_logger.debug("Using proxy settings: http.proxyHost='"+httpProxyHost+"', http.proxyPort='"+httpProxyPort+"'.");

		// Initialize this early...
		// If it's initalized after any connect attempt we might have
		// problems connecting to various destinations.
		CheckForUpdates.init();


		if (_gui)
		{
			// Calling this would make GuiLogAppender, to register itself in log4j.
			GuiLogAppender.getInstance();

			// How long should a ToolTip be displayed... 
			// this is especially good for WaitEventID tooltip help, which could be much text to read. 
			ToolTipManager.sharedInstance().setDismissDelay(120*1000); // 2 minutes
		}

		// Check registered LOG4J appenders
		// NOTE: this should NOT be here for production
		//Enumeration en = Logger.getRootLogger().getAllAppenders();
		//while (en.hasMoreElements())
		//{
		//	Appender a = (Appender) en.nextElement();
		//	System.out.println("Appender="+a);
		//	System.out.println("Appender.getName="+a.getName());
		//}

		
		// Print out the memory configuration
		// And the JVM info
		_logger.info("Starting "+Version.getAppName()+", version "+Version.getVersionStr()+", build "+Version.getBuildStr());
		
		_logger.info("Using Java Runtime Environment Version: "+System.getProperty("java.version"));
//		_logger.info("Using Java Runtime Environment Vendor: "+System.getProperty("java.vendor"));
//		_logger.info("Using Java Vendor URL: "+System.getProperty("java.vendor.url"));
//		_logger.info("Using Java VM Specification Version: "+System.getProperty("java.vm.specification.version"));
//		_logger.info("Using Java VM Specification Vendor:  "+System.getProperty("java.vm.specification.vendor"));
//		_logger.info("Using Java VM Specification Name:    "+System.getProperty("java.vm.specification.name"));
		_logger.info("Using Java VM Implementation  Version: "+System.getProperty("java.vm.version"));
		_logger.info("Using Java VM Implementation  Vendor:  "+System.getProperty("java.vm.vendor"));
		_logger.info("Using Java VM Implementation  Name:    "+System.getProperty("java.vm.name"));
		_logger.info("Using Java VM Home:    "+System.getProperty("java.home"));
		_logger.info("Java class format version number: " +System.getProperty("java.class.version"));
		_logger.info("Java class path: " +System.getProperty("java.class.path"));
		_logger.info("List of paths to search when loading libraries: " +System.getProperty("java.library.path"));
		_logger.info("Name of JIT compiler to use: " +System.getProperty("java.compiler"));
		_logger.info("Path of extension directory or directories: " +System.getProperty("java.ext.dirs"));
		
		_logger.info("Maximum memory is set to:  "+Runtime.getRuntime().maxMemory() / 1024 / 1024 + " MB. this could be changed with  -Xmx###m (where ### is number of MB)"); // jdk 1.4 or higher
		_logger.info("Running on Operating System Name:  "+System.getProperty("os.name"));
		_logger.info("Running on Operating System Version:  "+System.getProperty("os.version"));
		_logger.info("Running on Operating System Architecture:  "+System.getProperty("os.arch"));
		_logger.info("The application was started by the username:  "+System.getProperty("user.name"));
		_logger.info("The application was started in the directory:   "+System.getProperty("user.dir"));
		
		_logger.info("Using configuration file '"+propFile+"'.");
		_logger.info("Storing temporary configurations in file '"+savePropFile+"'.");


		// check if sufficent memory has been configgured.
		// [FIXME] what is a properiate default value here.
		String needMemInMBStr = "32";
		int maxConfigMemInMB = (int) Runtime.getRuntime().maxMemory() / 1024 / 1024; // jdk 1.4 or higher
		int needMemInMB = Integer.parseInt( asemonProps.getProperty("minMemoryLimitInMB", needMemInMBStr) );
		if (maxConfigMemInMB < needMemInMB)
		{
			String message = "I need atleast "+needMemInMB+" MB to start this process. Maximum memory limit is now configured to "+maxConfigMemInMB+" MB. Specify this at the JVM startup using the -Xmx###m flag. ### is the upper limit (in MB) that this JVM could use.";
			_logger.error(message);
			throw new Exception(message);
		}

		if ( ! _gui )
		{
			_logger.info("Starting asemon in NO-GUI mode, all counters will be sampled.");

			// Create and Start the "collector" thread
			getCnt = new GetCountersNoGui();
			getCnt.init();
			getCnt.start();

			//---------------------------------
			// Go and check for updates aswell.
			//---------------------------------
			_logger.info("Checking for new release...");
			CheckForUpdates.noBlockCheck(null, false, false);
		}
		else
		{
			_logger.info("Starting asemon in GUI mode.");

			try
			{
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}


			// Create the GUI
			MainFrame frame = new MainFrame();

			// Create and Start the "collector" thread
			getCnt = new GetCountersGui();
			getCnt.init();
			getCnt.start();

			if (packFrame)
			{
				frame.pack();
			}
			else
			{
				frame.validate();
			}

			int with    = Asemon.getSaveProps().getIntProperty("window.width", 1000);
			int height  = Asemon.getSaveProps().getIntProperty("window.height", 700);
			int winPosX = Asemon.getSaveProps().getIntProperty("window.pos.x",  -1);
			int winPosY = Asemon.getSaveProps().getIntProperty("window.pos.y",  -1);
			Dimension mySize = new Dimension(with, height);

			// Set last known size, or Set a LARGE size
			frame.setSize(mySize);

			//Center the window
			if (winPosX == -1  && winPosY == -1)
			{
				_logger.debug("Open main window in center of screen.");

				Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
				Dimension frameSize = frame.getSize();

				// We cant be larger than the screen
				if (frameSize.height > screenSize.height) frameSize.height = screenSize.height;
				if (frameSize.width  > screenSize.width)  frameSize.width  = screenSize.width;

				frame.setLocation((screenSize.width - frameSize.width) / 2,
				        (screenSize.height - frameSize.height) / 2);
			}
			// Set to last known position
			else
			{
				_logger.debug("Open main window in last known position.");
				frame.setLocation(winPosX, winPosY);
			}

			frame.setVisible(true);
			frame.addWindowListener( new WindowAdapter()
			{
				public void windowClosing(WindowEvent e)
				{
					//_logger.debug("xxxxx: " + e.getWindow().getSize());

					Asemon.getSaveProps().setProperty("window.width",  e.getWindow().getSize().width);
					Asemon.getSaveProps().setProperty("window.height", e.getWindow().getSize().height);
					Asemon.getSaveProps().setProperty("window.pos.x",  e.getWindow().getLocationOnScreen().x);
					Asemon.getSaveProps().setProperty("window.pos.y",  e.getWindow().getLocationOnScreen().y);
					Asemon.getSaveProps().save();
				}
			});

			//---------------------------------
			// Go and check for updates aswell.
			//---------------------------------
			_logger.info("Checking for new release...");
			CheckForUpdates.noBlockCheck(frame, false, false);

		} // end: gui code
	}

	public static Configuration getProps()
	{
		return asemonProps;
	}
	public static Configuration getSaveProps()
	{
		return asemonSaveProps;
	}
	public static Configuration getStoreProps()
	{
		return storeConfigProps;
	}

	public static GetCounters getCounterCollector()
	{
		return getCnt;
	}

	public static boolean hasGUI()
	{
		return _gui;
	}


	//Main method
	public static void main(String[] args)
	{
		String asemon_home = System.getProperty("ASEMON_HOME");
		String asemonPropsFile;
		String asemonSavePropsFile;

		if (asemon_home != null)
		{
			asemonPropsFile     = asemon_home + "/asemon.properties";
			asemonSavePropsFile = asemon_home + "/asemon.save.properties";
		}
		else
		{
			asemonPropsFile     = "asemon.properties";
			asemonSavePropsFile = "asemon.save.properties";
		}

		try
		{
			new Asemon(asemonPropsFile, asemonSavePropsFile);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
