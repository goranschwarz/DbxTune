package com.asetune.utils;

import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.Charset;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.asetune.Version;
import com.asetune.gui.swing.GTable;

public class JdbcDriverHelper
{
	private static Logger _logger = Logger.getLogger(JdbcDriverHelper.class);
	public final static String  DEFAULT_DriversFileName = Version.APP_STORE_DIR + File.separator + "JdbcDrivers.xml";

	public static final String JDBC_DRIVER_DOWNLOAD_URL = "http://www.asetune.com/jdbc_drivers_download.php";

	private static JdbcDriverTableModel _jdbcDriverModel = null;
	private static String               _filename        = DEFAULT_DriversFileName;
	
	public static JdbcDriverTableModel getModel()
	{
		if (_jdbcDriverModel == null)
			_jdbcDriverModel = new JdbcDriverTableModel();

		return _jdbcDriverModel;
	}

	/**
	 * Get a list of available JDBC classes that is in the classpath or the XML File, which describes JDBC Drivers 
	 * @return
	 */
	public static List<String> getAvailableDriverList()
	{
		ArrayList<String> list = new ArrayList<String>();

		JdbcDriverTableModel model = getModel();
		for (DriverInfoEntry e : model.getEntries(true))
			list.add(e.getClassName());

		return list;
	}
	/**
	 * Get URL Templates for a specific driver
	 * @param driverName
	 * @return
	 */
	public static List<String> getUrlTemplateList(String driverName)
	{
		JdbcDriverTableModel model = getModel();
		DriverInfoEntry entry = model.getEntry(driverName);
		if (entry != null)
			return entry.getUrlTemplateList();
		
		return getDefaultUrlTemplateList(driverName);
	}
	/**
	 * Get some URL templates for various drivers.
	 */
	public static List<String> getDefaultUrlTemplateList(String driverName)
	{
		// Entry wasn't found: add some static entries found on the Internet
		ArrayList<String> templates = new ArrayList<String>();
		
		if ("sun.jdbc.odbc.JdbcOdbcDriver".equals(driverName)) 
		{ 
			templates.add("jdbc:odbc:DSN");
		}
		else if (    "com.sybase.jdbc3.jdbc.SybDriver".equals(driverName) 
		          || "com.sybase.jdbc4.jdbc.SybDriver".equals(driverName) ) 
		{
			templates.add("jdbc:sybase:Tds:<host>:<port>");
			templates.add("jdbc:sybase:Tds:<host>:<port>[/<dbname>]");
//			templates.add("jdbc:sybase:Tds:<host>:<port>[/<dbname>][?APPLICATIONNAME=testapp&OPT2=value]");
//			templates.add("jdbc:sybase:Tds:<host1>:<port1>[,<host2>:<port2>,<hostN>:<portN>][/<dbname>]");
			templates.add("jdbc:sybase:Tds:<host>:<port>?OPT1=val&OPT2=val");
		}
		else if ("net.sourceforge.jtds.jdbc.Driver".equals(driverName))
		{
			templates.add("jdbc:jtds:<server_type>://<host>[:<port>][/<db>][;<prop>=<val>[;...]]");
			templates.add("jdbc:jtds:sybase://<host>[:<port>][/<db>][;<prop>=<val>[;...]]");
			templates.add("jdbc:jtds:sqlserver://<host>[:<port>][/<db>][;<prop>=<val>[;...]]");
		}
		else if ("org.h2.Driver".equals(driverName))
		{
			templates.add("jdbc:h2:file:[<path>]<dbname>");
			templates.add("jdbc:h2:file:[<path>]<dbname>;IFEXISTS=TRUE");
			templates.add("jdbc:h2:file:[<path>]<dbname>;AUTO_SERVER=TRUE");
//			templates.add("jdbc:h2:file:[<path>]<dbname>;IFEXISTS=TRUE;AUTO_SERVER=TRUE");
			templates.add("jdbc:h2:file:${ASETUNE_SAVE_DIR}/${SERVERNAME}_${DATE}");
//			templates.add("jdbc:h2:file:${ASETUNE_SAVE_DIR}/${SERVERNAME}_${DATE};AUTO_SERVER=TRUE");
			templates.add("jdbc:h2:file:${ASETUNE_SAVE_DIR}/${ASEHOSTNAME}_${DATE}");
//			templates.add("jdbc:h2:file:${ASETUNE_SAVE_DIR}/${ASEHOSTNAME}_${DATE};AUTO_SERVER=TRUE");
			templates.add("jdbc:h2:tcp://<host>[:<port>]/<dbname>");
			templates.add("jdbc:h2:ssl://<host>[:<port>]/<dbname>");
			templates.add("jdbc:h2:zip:<zipFileName>!/<dbname>");
		}
		else if ("com.sap.db.jdbc.Driver".equals(driverName)) 
		{
			templates.add("jdbc:sap://<host>:<port>");
			templates.add("jdbc:sap://<host>:3##15");
			templates.add("jdbc:sap://<host>:3##15 (replace## with instance_number)");
			templates.add("jdbc:sap://<host>:30015");
		}
		else if ("oracle.jdbc.OracleDriver".equals(driverName)) 
		{
			templates.add("jdbc:oracle:thin:@//[HOST][:PORT]/SERVICE");
			templates.add("jdbc:oracle:thin:@[HOST][:PORT]:SID");
		}
		else if ("com.microsoft.jdbc.sqlserver.SQLServerDriver".equals(driverName)) //    (Microsoft SQL Server JDBC Driver ) 
		{
			templates.add("jdbc:microsoft:sqlserver://<host>:<port> ");
			templates.add("jdbc:microsoft:sqlserver://<host>:<port>;databasename=name");
		}
		else if ("com.microsoft.sqlserver.jdbc.SQLServerDriver".equals(driverName)) //    (Microsoft SQL Server 2005 JDBC Driver ) 
		{
			templates.add("jdbc:sqlserver://<host>:<port> ");
		}
		else if ("org.postgresql.Driver".equals(driverName)) 
		{
			templates.add("jdbc:postgresql:database");
			templates.add("jdbc:postgresql://host/database");
			templates.add("jdbc:postgresql://host:port/database");
			templates.add("jdbc:postgresql://host:port/database?user=userName&password=pass");
			templates.add("jdbc:postgresql://host:port/database?charSet=LATIN1&compatible=7.2");
		}
		else if ("com.mysql.jdbc.Driver".equals(driverName)) 
		{
			templates.add("jdbc:mysql://[host][:port]/[database][?p1=v1]...");
			templates.add("jdbc:mysql://[host][,failoverhost...][:port]/[database]");
			templates.add("jdbc:mysql://[host][,failoverhost...][:port]/[database][?propertyName1][=propertyValue1][&propertyName2][=propertyValue2]...");
		}
		else if ("org.apache.derby.jdbc.EmbeddedDriver".equals(driverName)) 
		{
			templates.add("jdbc:derby://host/database");
		}
		else if ("org.apache.derby.jdbc.ClientDriver".equals(driverName)) 
		{
			templates.add("jdbc:derby://host/database");
		}

		return templates;
	}

	public static String getDescription(String driverName)
	{
		JdbcDriverTableModel model = getModel();
		DriverInfoEntry entry = model.getEntry(driverName);
		if (entry != null)
			return entry.getDescription();
		
		return getDefaultDescription(driverName);
	}

	public static String getDefaultDescription(String driverName)
	{
		if      ("sun.jdbc.odbc.JdbcOdbcDriver"                .equals(driverName)) return "JDBC - ODBC Bridge";
		else if ("com.sybase.jdbc3.jdbc.SybDriver"             .equals(driverName))	return "Sybase JDBC 3.0 Driver";
		else if ("com.sybase.jdbc4.jdbc.SybDriver"             .equals(driverName))	return "Sybase JDBC 4.0 Driver";
		else if ("net.sourceforge.jtds.jdbc.Driver"            .equals(driverName))	return "jTDS JDBC Driver";
		else if ("org.h2.Driver"                               .equals(driverName))	return "H2 JDBC Driver";
		else if ("com.sap.db.jdbc.Driver"                      .equals(driverName))	return "SAP HANA JDBC Driver";
		else if ("oracle.jdbc.OracleDriver"                    .equals(driverName))	return "Oracle JDBC Driver";
		else if ("com.microsoft.jdbc.sqlserver.SQLServerDriver".equals(driverName)) return "Microsoft SQL Server JDBC Driver"; 
		else if ("com.microsoft.sqlserver.jdbc.SQLServerDriver".equals(driverName)) return "Microsoft SQL Server 2005 JDBC Driver"; 
		else if ("org.postgresql.Driver"                       .equals(driverName))	return "Postgres JDBC Driver";
		else if ("com.mysql.jdbc.Driver"                       .equals(driverName))	return "MySQL JDBC Driver";
		else if ("org.apache.derby.jdbc.EmbeddedDriver"        .equals(driverName))	return "Derby Embedded JDBC Driver";
		else if ("org.apache.derby.jdbc.ClientDriver"          .equals(driverName))	return "Derby Client JDBC Driver";

		return "";
	}

	public static String getHomePage(String driverName)
	{
		JdbcDriverTableModel model = getModel();
		DriverInfoEntry entry = model.getEntry(driverName);
		if (entry != null)
			return entry.getHomePage();
		
		return getDefaultHomePage(driverName);
	}

	public static String getDefaultHomePage(String driverName)
	{
		if      ("sun.jdbc.odbc.JdbcOdbcDriver"                .equals(driverName)) return "en.wikipedia.org/wiki/JDBC_driver";
		else if ("com.sybase.jdbc3.jdbc.SybDriver"             .equals(driverName))	return "www.sybase.com/jconnect";
		else if ("com.sybase.jdbc4.jdbc.SybDriver"             .equals(driverName))	return "www.sybase.com/jconnect";
		else if ("net.sourceforge.jtds.jdbc.Driver"            .equals(driverName))	return "jtds.sourceforge.net";
		else if ("org.h2.Driver"                               .equals(driverName))	return "www.h2database.com";
		else if ("com.sap.db.jdbc.Driver"                      .equals(driverName))	return "www.sap.com/HANA";
		else if ("oracle.jdbc.OracleDriver"                    .equals(driverName))	return "www.oracle.com/technetwork/database/features/jdbc";
		else if ("com.microsoft.jdbc.sqlserver.SQLServerDriver".equals(driverName)) return "msdn.microsoft.com/en-US/sqlserver/aa937724"; 
		else if ("com.microsoft.sqlserver.jdbc.SQLServerDriver".equals(driverName)) return "msdn.microsoft.com/en-US/sqlserver/aa937724"; 
		else if ("org.postgresql.Driver"                       .equals(driverName))	return "jdbc.postgresql.org";
		else if ("com.mysql.jdbc.Driver"                       .equals(driverName))	return "www.mysql.com/products/connector";
		else if ("org.apache.derby.jdbc.EmbeddedDriver"        .equals(driverName))	return "db.apache.org/derby";
		else if ("org.apache.derby.jdbc.ClientDriver"          .equals(driverName))	return "db.apache.org/derby";

		return "";
	}


	private static class DriverWrapper implements Driver
	{
		private Driver	     _driver;
		private String       _jarFile;
		private String       _homePage;
		private String       _desc;
		private List<String> _urlTemplateList;

		DriverWrapper(Driver driver)
		{
			_driver = driver;
		}

		public static void install(String className, String jarFile, String homePage, String desc, List<String> urlTemplateList) 
		throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException, MalformedURLException
		{
			// Fix a class loader
			// FIXME: the URLClassLoader first searched the Parent ClassLoader, so if any JARS are part 
			//        of the CLASSPATH it will still pick it up from there...
			//        So maybe create our own class loader, which FIRTS checks the JAR file, then the CLASSPATH
			URL url = new URL("jar:file:"+jarFile+"!/");
//			URLClassLoader cl = new URLClassLoader(new URL[] { url });

			ArrayList<URL> urlList = new ArrayList<URL>(); 
			urlList.add(url);
			ParentLastURLClassLoader cl = new ParentLastURLClassLoader( urlList );

			// Load the driver, but use a Wrapper otherwise it will fail to load
			Driver driver = (Driver) Class.forName(className, true, cl).newInstance();
			DriverWrapper driverWrapper = new DriverWrapper(driver);

			// FINE the class could be loaded
			// now lets de-register all drivers with same driver name...
			deregisterDriver(className);

			// Finally register the new driver
//System.out.println("INSTALL: REGISTER DRIVER: className='"+className+"', jarfile='"+jarFile+"'.");
			DriverManager.registerDriver(driverWrapper);

			// Set some properties, which will be used when getting info from the Model
			driverWrapper._driver   = driver;
			driverWrapper._jarFile  = jarFile;
			driverWrapper._homePage = homePage;
			driverWrapper._desc     = desc;
			driverWrapper._urlTemplateList = (urlTemplateList != null) ? urlTemplateList : new ArrayList<String>();
		}

		public static void deregisterDriver(String className)
		{
			// FINE the class could be loaded
			// now lets de-register all drivers with same driver name...
			for (Enumeration<Driver> driversEnum = DriverManager.getDrivers(); driversEnum.hasMoreElements();)
			{
				Driver dmDriver    = driversEnum.nextElement();
				String dmClassName = dmDriver.getClass().getName();
	
				// If it's already a Wrapper lets get the "origin" class name
				if (dmDriver instanceof JdbcDriverHelper.DriverWrapper)
					dmClassName = ((JdbcDriverHelper.DriverWrapper) dmDriver).getClassName();
	
				if (dmClassName.equals(className))
				{
					try
					{
//System.out.println("INSTALL: DE-REGISTER DRIVER: dmClassName='"+dmClassName+"', dmDriver='"+dmDriver+"', driver.class='"+driver.getClass().getName()+"'.");
						DriverManager.deregisterDriver(dmDriver);
					}
					catch (SQLException ex)
					{
						_logger.warn("Problems de-register driver='"+dmDriver+"'. Caught: "+ex);
					}
				}
			}
		}

		@Override
		public boolean acceptsURL(String u) throws SQLException
		{
			return _driver.acceptsURL(u);
		}

		@Override
		public Connection connect(String u, Properties p) throws SQLException
		{
			return _driver.connect(u, p);
		}

		@Override
		public int getMajorVersion()
		{
			return _driver.getMajorVersion();
		}

		@Override
		public int getMinorVersion()
		{
			return _driver.getMinorVersion();
		}

		@Override
		public DriverPropertyInfo[] getPropertyInfo(String u, Properties p) throws SQLException
		{
			return _driver.getPropertyInfo(u, p);
		}

		@Override
		public boolean jdbcCompliant()
		{
			return _driver.jdbcCompliant();
		}

		@Override
		public String toString()
		{
			return _driver.toString();
		}

		/** Get the original Drivers class */
		public Driver getDriver()
		{
			return _driver;
		}
		/** Get the original Drivers classname */
		public String getClassName()
		{
			return _driver.getClass().getName();
		}

		/** what jar file  */
		public String getJarFile()
		{
			return _jarFile;
		}

		/** get simple description */
		public String getDecription()
		{
			return _desc;
		}

		/** get Home page  */
		public String getHomePage()
		{
			return _homePage;
		}

		/** get Home page  */
		public List<String> getUrlTemplateList()
		{
			return _urlTemplateList;
		}
	}

	/**
	 * A parent-last classloader that will try the child classloader first and then the parent.
	 * This takes a fair bit of doing because java really prefers parent-first.
	 * 
	 * For those not familiar with class loading trickery, be wary
	 * grabbed from: http://stackoverflow.com/questions/5445511/how-do-i-create-a-parent-last-child-first-classloader-in-java-or-how-to-overr 
	 */
	private static class ParentLastURLClassLoader extends ClassLoader
	{
		private ChildURLClassLoader	childClassLoader;

		/**
		 * This class allows me to call findClass on a classloader
		 */
		private static class FindClassClassLoader extends ClassLoader
		{
			public FindClassClassLoader(ClassLoader parent)
			{
				super(parent);
			}

			@Override
			public Class<?> findClass(String name) throws ClassNotFoundException
			{
				return super.findClass(name);
			}
		}

		/**
		 * This class delegates (child then parent) for the findClass method for
		 * a URLClassLoader. We need this because findClass is protected in
		 * URLClassLoader
		 */
		private static class ChildURLClassLoader extends URLClassLoader
		{
			private FindClassClassLoader	realParent;

			public ChildURLClassLoader(URL[] urls, FindClassClassLoader realParent)
			{
				super(urls, null);

				this.realParent = realParent;
			}

			@Override
			public Class<?> findClass(String name) throws ClassNotFoundException
			{
				try
				{
					// first try to use the URLClassLoader findClass
					return super.findClass(name);
				}
				catch (ClassNotFoundException e)
				{
					// if that fails, we ask our real parent classloader to load
					// the class (we give up)
					return realParent.loadClass(name);
				}
			}
		}

		public ParentLastURLClassLoader(List<URL> classpath)
		{
			super(Thread.currentThread().getContextClassLoader());

			URL[] urls = classpath.toArray(new URL[classpath.size()]);

			childClassLoader = new ChildURLClassLoader(urls, new FindClassClassLoader(this.getParent()));
		}

		@Override
		protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
		{
			try
			{
				// first we try to find a class inside the child classloader
				return childClassLoader.findClass(name);
			}
			catch (ClassNotFoundException e)
			{
				// didn't find it, try the parent
				return super.loadClass(name, resolve);
			}
		}
	}
	
//	private static class Test
//	{
//		String jdbcJar   = System.getProperty("SQLW_HOME")+"/lib/jtds-1.2.7.jar";
//		String jdbcClass = "net.sourceforge.jtds.jdbc.Driver";
////		String jdbcUrl   = "jdbc:jtds:sybase://<host>[:<port>][/<database>]";
//		String jdbcUrl   = "jdbc:jtds:sybase://localhost:5000";
//		String jdbcUser  = "sa";
//		String jdbcPass  = "";
//
//		public void willNotWork() throws MalformedURLException, ClassNotFoundException, SQLException
//		{
//			URL u = new URL("jar:file:"+jdbcJar+"!/");
//			String classname = jdbcClass;
//			URLClassLoader ucl = new URLClassLoader(new URL[] { u });
//			Class.forName(classname, true, ucl);
//			DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPass);
//			// That will throw SQLException: No suitable driver
//
//			Connection conn = DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPass);
//			System.out.println("ActiveRoles 1: " + AseConnectionUtils.getActiveRoles(conn));
//		}
//
//		public void willWork() throws MalformedURLException, ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException
//		{
//			URL u = new URL("jar:file:"+jdbcJar+"!/");
//			String classname = jdbcClass;
//			URLClassLoader ucl = new URLClassLoader(new URL[] { u });
//
//			Driver d = (Driver) Class.forName(classname, true, ucl).newInstance();
//			DriverManager.registerDriver(new DriverWrapper(d));
//
//			Connection conn = DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPass);
//			System.out.println("ActiveRoles 2: " + AseConnectionUtils.getActiveRoles(conn));
//			// Success!
//		}
//	}

//	private static void printDrivers()
//	{
//		for (Enumeration<Driver> drivers = DriverManager.getDrivers(); drivers.hasMoreElements();)
//		{
//			Driver driver = drivers.nextElement();
//			if (drivers instanceof DriverWrapper)
//				System.out.println("DRIVER Wrapper : class='"+((DriverWrapper)driver).getClassName()+"', toString="+driver.toString());
//			else
//				System.out.println("DRIVER for-real: class='"+driver.getClass().getName()+"', toString="+driver.toString());
//		}
//	}

	/**
	 * Get a list of registered drivers from the DriverManager.getDrivers()
	 * @return a List<String> with the class names that is registered
	 */
	public static List<String> getDriverList()
	{
		ArrayList<String> driverList = new ArrayList<String>();
		
		for (Enumeration<Driver> drivers = DriverManager.getDrivers(); drivers.hasMoreElements();)
		{
			Driver driver = drivers.nextElement();

			if (drivers instanceof DriverWrapper)
				driverList.add( ((DriverWrapper)driver).getClassName() );
			else
				driverList.add( driver.getClass().getName() );
		}
		
		return driverList;
	}


	/**
	 * Small JPanel with a JTable that holds currently loaded JDBC Drivers (registered in DriverManager)
	 * <p>
	 * You may add, remove drivers
	 * 
	 * @author gorans
	 */
	public static class JdbcDriverInfoPanel
	extends JPanel
	{
		private static final long serialVersionUID = 1L;

		private GTable               _table         = new GTable();
		private JdbcDriverTableModel _tm            = null;
		private JLabel               _xmlFile       = new JLabel(getFileName());
		private JButton              _reload_but    = new JButton("Reload");
		private JButton              _download_but  = new JButton("Download");
		private JButton              _addDriver_but = new JButton("Add/Change Driver");
		private JButton              _delDriver_but = new JButton("Delete Driver");

		public JdbcDriverInfoPanel()
		{
			init();
		}
		private String getDriversPath()
		{
			File driversDir = new File(Version.APP_STORE_DIR + File.separator + "jdbc_drivers");
			if ( ! driversDir.exists() )
			{
				if (driversDir.mkdir())
					_logger.info("Creating directory '"+driversDir+"' to hold JDBC Driver files for "+Version.getAppName());
			}
			
			return driversDir.toString();
		}
		private void init()
		{
			String driversDir = getDriversPath();
			_xmlFile      .setToolTipText("This is where JDBC Drivers not included in the classpath will be described.");
			_reload_but   .setToolTipText("Reload JDBC Drivers from the above XML file");
			_download_but .setToolTipText("<html>Download various JDBC drivers<br>"+Version.getAppName()+" can't distribute a lot of JDBC Drivers, so you need to download them yourself.<br><br>This will just open a web page that has a collection of various JDBC Drivers that can be downloaded.<br>Put them in the directory <code>"+driversDir+"</code> and restart "+Version.getAppName()+".</html>");
			_addDriver_but.setToolTipText("Open a Dialog to add a JDBC Driver");
			_delDriver_but.setToolTipText("Delete the selected Driver in the list");

			JScrollPane scroll = new JScrollPane();
			scroll.setViewportView(_table);

			_tm = getModel();
//			_tm = new JdbcDriverTableModel();

			_table.setModel(_tm);
			_table.setSortable(true);
			_table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			_table.setColumnControlVisible(true);
			_table.packAll();
			
			// make this low, otherwise it will grow to much because of any outer JScrollPane
			_table.setPreferredScrollableViewportSize(new Dimension(400, 100));

			setLayout(new MigLayout("insets 0 0 0 0"));   // insets Top Left Bottom Right

			add(scroll,          "span, push, grow, wrap");
			add(_xmlFile,        "span, pushx, growx, wrap");
			add(_reload_but,     "");
			add(new JLabel(),    "span, split, growx, pushx"); // dummy ...
			add(_download_but,   "");
			add(_addDriver_but,  "");
			add(_delDriver_but,  "wrap");

//			_addDriver_but.setEnabled(false);
//			_addDriver_but.setToolTipText("Sorry not yet implemented...");

			_reload_but.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					_tm.reload();
					_table.packAll();
				}
			});

			_download_but.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					if (Desktop.isDesktopSupported())
					{
						Desktop desktop = Desktop.getDesktop();
						if ( desktop.isSupported(Desktop.Action.BROWSE) )
						{
							_logger.info("You clicked on Download Drivers '"+JDBC_DRIVER_DOWNLOAD_URL+"'. Browser will be opened.");  

							try
							{
								desktop.browse(new URI(JDBC_DRIVER_DOWNLOAD_URL+"?toLocation="+URLEncoder.encode(getDriversPath(), "UTF-8")));
							}
							catch (Exception ex)
							{
								_logger.error("Problems when open the URL '"+JDBC_DRIVER_DOWNLOAD_URL+"'. Caught: "+ex);
							}
						}
					}
				}
			});

			_addDriver_but.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					int vrow = _table.getSelectedRow();
					if (vrow >= 0) // CHANGE ENTRY
					{
						int mrow = _table.convertRowIndexToModel(vrow);
						DriverInfoEntry entry = _tm.getEntry(mrow);

						if (entry.getInSystemPath())
						{
							SwingUtils.showInfoMessage("In System Class Path", "<html>Can't change properties for a JDBC Driver in the System Class Path<br>So the entry was <b>cloned</b> with a new name</html>");

							DriverInfoEntry cloneEntry = DriverInfoEntry.cloneEntry(entry);

							DriverInfoEntry retEntry = AddOrChangeEntryDialog.showDialog(null, cloneEntry);
							if (retEntry != null)
								_tm.addEntry(retEntry);

							saveFile(true);
						}
						else
						{
							DriverInfoEntry retEntry = AddOrChangeEntryDialog.showDialog(null, entry);
							if (retEntry != null)
								_tm.fireTableDataChanged();

							saveFile(true);
						}
					}
					else // NEW ENTRY
					{
						DriverInfoEntry retEntry = AddOrChangeEntryDialog.showDialog(null, null);
						if (retEntry != null)
							_tm.addEntry(retEntry);

						saveFile(true);
					}
					_reload_but.doClick();
				}
			});
			
			_delDriver_but.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					int vrow = _table.getSelectedRow();
					if (vrow >= 0)
					{
						int mrow = _table.convertRowIndexToModel(vrow);
						DriverInfoEntry entry = _tm.getEntry(mrow);

						if (entry.getInSystemPath())
						{
							SwingUtils.showInfoMessage("In System Class Path", "Sorry Can't delete a JDBC Driver in the System Class Path");
						}
						else
						{
							_tm.deleteEntry(mrow, true);
							saveFile(true);
						}
					}
					else
						SwingUtils.showInfoMessage(null, "Select a row", "Please select a row you want to delete");

					_reload_but.doClick();
				}
			});
		}
	}



	//-------------------------------------------------------------------
	// Local private classes
	//-------------------------------------------------------------------
	public static class DriverInfoEntry
	{
		private boolean _inSystemPath = false;
		private String  _className    = null;
		private String  _description  = null;
		private String  _homePage     = null;
		private String  _version      = null;
		private String  _jarFile      = null;
		private String  _toString     = null;

//		private String       _urlTemplate      = null; // URL Template
		private List<String> _urlTemplateList  = new ArrayList<String>();

		public DriverInfoEntry()
		{
		}

		public static DriverInfoEntry cloneEntry(DriverInfoEntry entry)
		{
			DriverInfoEntry cloned = new DriverInfoEntry();

			cloned.setInSystemPath   (false);
			cloned.setClassName      ("Cloned: " + entry.getClassName());
			cloned.setDescription    ("Cloned: " + entry.getDescription());
			cloned.setHomePage       ("Cloned: " + entry.getHomePage());
			cloned.setVersion        (entry.getVersion());
			cloned.setJarFile        (entry.getJarFile().replace("file:/", ""));
			cloned.setUrlTemplateList(entry.getUrlTemplateList());
			return cloned;
		}

		public boolean getInSystemPath() { return _inSystemPath;   }
		public String  getClassName()    { return _className;   }
		public String  getDescription()  { return _description; }
		public String  getHomePage()     { return _homePage;    }
		public String  getVersion()      { return _version;     }
		public String  getJarFile()      { return _jarFile;     }
		public String  getToString()     { return _toString;    }

//		public String        getUrlTemplate()     { return _urlTemplate; }
		public List<String>  getUrlTemplateList() { return _urlTemplateList; }

		public void setInSystemPath(boolean inSystemPath)
		{
			_inSystemPath = inSystemPath;
		}

		public void setClassName(String className)
		{
			if (className == null)
				className = "";
			_className = className;
		}

		public void setDescription(String description)
		{
			if (description == null)
				description = "";
			_description = description;
		}

		public void setHomePage(String homePage)
		{
			if (homePage == null)
				homePage = "";
			_homePage = homePage;
		}

		public void setVersion(String version)
		{
			if (version == null)
				version = "";
			_version = version;
		}

		public void setJarFile(String jarFile)
		{
			if (jarFile == null)
				jarFile = "";
			_jarFile = jarFile;
		}

		public void setToString(String toString)
		{
			if (toString == null)
				toString = "";
			_toString = toString;
		}

		public void setUrlTemplateList(List<String> urlTemplates)
		{
			if (urlTemplates == null)
				urlTemplates = new ArrayList<String>();
			_urlTemplateList = urlTemplates;
		}
		public void addUrlTemplate(String urlTemplate)
		{
			if (urlTemplate == null)
				urlTemplate = "";
			_urlTemplateList.add(urlTemplate);
		}

		public String toXml()
		{
			// Add entry
			StringBuilder sb = new StringBuilder();
				
			sb.append("\n");
			sb.append("    ").append(XML_BEGIN_TAG_DRIVER_ENTRY).append("\n");
			sb.append("        ").append(XML_BEGIN_SUBTAG_CLASSNAME)  .append(StringUtil.xmlSafe(getClassName()    )).append(XML_END___SUBTAG_CLASSNAME)  .append("\n");
			sb.append("        ").append(XML_BEGIN_SUBTAG_DESCRIPTION).append(StringUtil.xmlSafe(getDescription()  )).append(XML_END___SUBTAG_DESCRIPTION).append("\n");
			sb.append("        ").append(XML_BEGIN_SUBTAG_HOME_PAGE)  .append(StringUtil.xmlSafe(getHomePage()     )).append(XML_END___SUBTAG_HOME_PAGE)  .append("\n");
			sb.append("        ").append(XML_BEGIN_SUBTAG_JAR_FILE)   .append(StringUtil.xmlSafe(getJarFile()      )).append(XML_END___SUBTAG_JAR_FILE)   .append("\n");
			for (String urlTemplate : getUrlTemplateList())
				sb.append("        ").append(XML_BEGIN_SUBTAG_URL_TEMPLATE).append(StringUtil.xmlSafe(urlTemplate)).append(XML_END___SUBTAG_URL_TEMPLATE)    .append("\n");
			sb.append("    ").append(XML_END___TAG_DRIVER_ENTRY).append("\n");

			return sb.toString();
		}
	}
	

	public static class JdbcDriverTableModel
	extends AbstractTableModel
	{
		private static final long serialVersionUID = 1L;

		private static final String[] TAB_HEADER = {"System", "Class", "Description", "Home Page", "Jar File", "toString", "Version"};
		private static final int TAB_POS_SYSTEM    = 0;
		private static final int TAB_POS_CLASS     = 1;
		private static final int TAB_POS_DESC      = 2;
		private static final int TAB_POS_HOME_PAGE = 3;
		private static final int TAB_POS_JAR_FILE  = 4;
		private static final int TAB_POS_TO_STRING = 5;
		private static final int TAB_POS_VERSION   = 6;

		private ArrayList<DriverInfoEntry> _rows = new ArrayList<DriverInfoEntry>();
		private boolean _hasChanged = false;

		public JdbcDriverTableModel()
		{
			loadModel(true);
		}

		public boolean isChanged()
		{
			return _hasChanged;
		}

		public void setChanged(boolean changed)
		{
			_hasChanged = changed;
		}

		public void clear(boolean fireChange)
		{
			_rows.clear();
			setChanged(true);
			if (fireChange)
				fireTableDataChanged();
		}

		public void reload()
		{
			clear(false);
			loadModel(true);
		}

		public DriverInfoEntry getEntry(String driver)
		{
			for (DriverInfoEntry entry : _rows)
			{
				if ( entry.getClassName().equals(driver) )
					return entry;
			}
			return null;
		}

		public DriverInfoEntry getEntry(int mrow)
		{
			return _rows.get(mrow);
		}

		public ArrayList<DriverInfoEntry> getEntries(boolean includeSystemPath)
		{
			ArrayList<DriverInfoEntry> retRows = new ArrayList<DriverInfoEntry>();
			for (DriverInfoEntry entry : _rows)
			{
				if (includeSystemPath && entry.getInSystemPath())
					retRows.add(entry);
				else
					retRows.add(entry);
			}
			return retRows;
		}

		public void deleteEntry(int mrow, boolean deRegisterInDriverManager)
		{
			if (deRegisterInDriverManager)
			{
				DriverInfoEntry entry = getEntry(mrow);
				if (entry != null)
					DriverWrapper.deregisterDriver(entry.getClassName());
			}
			
			_rows.remove(mrow);
			setChanged(true);
			fireTableDataChanged();
		}

		public void setEntries(ArrayList<DriverInfoEntry> entries, boolean fireChange)
		{
			_rows = entries;
			setChanged(true);
			if (fireChange)
				fireTableDataChanged();
		}

		public void addEntry(DriverInfoEntry entry)
		{
			_rows.add(entry);
			setChanged(true);
			fireTableDataChanged();
		}

		@Override
		public int getColumnCount() 
		{
			return TAB_HEADER.length;
		}

		@Override
		public String getColumnName(int column) 
		{
			switch (column)
			{
			case TAB_POS_SYSTEM:    return TAB_HEADER[TAB_POS_SYSTEM];
			case TAB_POS_CLASS:     return TAB_HEADER[TAB_POS_CLASS];
			case TAB_POS_DESC:      return TAB_HEADER[TAB_POS_DESC];
			case TAB_POS_HOME_PAGE: return TAB_HEADER[TAB_POS_HOME_PAGE];
			case TAB_POS_JAR_FILE:  return TAB_HEADER[TAB_POS_JAR_FILE];
			case TAB_POS_TO_STRING: return TAB_HEADER[TAB_POS_TO_STRING];
			case TAB_POS_VERSION:   return TAB_HEADER[TAB_POS_VERSION];
			}
			return null;
		}

		@Override
		public boolean isCellEditable(int row, int column)
		{
			if (column == TAB_POS_SYSTEM) 
				return false;
			return true;
		}

		@Override
		public int getRowCount()
		{
			return _rows.size();
		}

		@Override
		public Object getValueAt(int row, int column)
		{
			DriverInfoEntry entry = _rows.get(row);
			switch (column)
			{
			case TAB_POS_SYSTEM:    return entry.getInSystemPath();
			case TAB_POS_CLASS:     return entry.getClassName();
			case TAB_POS_DESC:      return entry.getDescription();
			case TAB_POS_HOME_PAGE: return entry.getHomePage();
			case TAB_POS_JAR_FILE:  return entry.getJarFile();
			case TAB_POS_TO_STRING: return entry.getToString();
			case TAB_POS_VERSION:   return entry.getVersion();
			}
			return null;
		}

		@Override
		public Class<?> getColumnClass(int column)
		{
			if (column == TAB_POS_SYSTEM) 
				return Boolean.class;

			return super.getColumnClass(column);
		}

		private void loadModel(boolean parseXmlFile)
		{
			// Parse the XML File
			if (parseXmlFile)
			{
				// If the file doesn't exist, try to create an empty one, with a small example
				File checkFile = new File(getFileName());
				if ( ! checkFile.exists() )
				{
					_logger.info("User Defined JDBC Drivers file '"+getFileName()+"', doesn't exist, try to create an empty.");
					ArrayList<DriverInfoEntry> empty = new ArrayList<DriverInfoEntry>();
					try { saveFile(getFileName(), empty); }
					catch (IOException ex) { _logger.warn("Problems creating: User Defined JDBC Drivers file '"+getFileName()+"'. Disregarding this. Caught: "+ex); }
				}

				JdbcDriverFileXmlParser parser = new JdbcDriverFileXmlParser();
				ArrayList<DriverInfoEntry> parsedEntries = parser.parseFile(getFileName());
	
				_logger.info("Get User Defined JDBC Drivers by parsing the file '"+getFileName()+"', which contained "+parsedEntries.size()+" entries.");
	
				//-------------------------------------------------------
				// First install all entries from the XML file
				for (DriverInfoEntry entry : parsedEntries)
				{
					// The install will de-register all classes in the current Driver Manager with the same class name 
					try
					{
						DriverWrapper.install(
								entry.getClassName(), 
								entry.getJarFile(), 
								entry.getHomePage(), 
								entry.getDescription(), 
								entry.getUrlTemplateList());
					}
					catch (Exception ex)
					{
						_logger.warn("Problems installing JDBC Driver='"+entry.getClassName()+"', JarFile='"+entry.getJarFile()+"'. This one will simply not be available. Caught: "+ex);
					}
				}
			}

			//-------------------------------------------------------
			// When they are Registered in the DriverManager
			// Get the info from there...
			for (Enumeration<Driver> drivers = DriverManager.getDrivers(); drivers.hasMoreElements();)
			{
				boolean inSystemPath   = true;
				Driver driver          = drivers.nextElement();
				String className       = driver.getClass().getName();
				String jarFile         = "<in system classpath>";
				String desc            = "";
				String homePage        = "";
				String version         = "";
				List<String> templates = new ArrayList<String>();

				_logger.debug("DriverManager-Entry: classname='"+className+"', driver='"+driver+"'.");

				if (driver instanceof JdbcDriverHelper.DriverWrapper)
				{
					DriverWrapper w = (DriverWrapper)driver;

					inSystemPath = false;

					className = w.getClassName();
					jarFile   = w.getJarFile();

					desc      = w.getDecription();
					homePage  = w.getHomePage();
					templates = w.getUrlTemplateList();

					driver    = w.getDriver();

					_logger.debug("DRIVER IS A WRAPPER. classname='"+className+"', jarFile='"+jarFile+"'.");
				}
				else
				{
					inSystemPath = true;

					// Get some "hard coded" values since, they will NOT be part of the XML file
					desc      = getDefaultDescription    (className);
					homePage  = getDefaultHomePage       (className);
					templates = getDefaultUrlTemplateList(className);
				}

				// Try to get JAR file which a class exists in (even for the ones in the class path))
				ProtectionDomain protDom = driver.getClass().getProtectionDomain();
				if (protDom != null)
				{
					CodeSource src = protDom.getCodeSource();
//System.out.println("DriverManager-Entry: CLASSLOADER='"+protDom.getClassLoader()+"', classname='"+className+"', driver='"+driver+"'.");

					if (src != null)
					{
						URL jar = src.getLocation();
						jarFile = jar + "";

						_logger.debug("DriverManager-Entry: classname='"+className+"', driver='"+driver+"' can be located in JAR File '"+jarFile+"'.");
					}
				}

				// Try to find out version of the driver in some way
				// The only way I can come up with is PropertyInfo and search for VERSION
				try
				{
					DriverPropertyInfo[] dpi = driver.getPropertyInfo("", null);
					if (dpi != null)
					{
						for (int i=0; i<dpi.length; i++)
						{
							String dpiName  = dpi[i].name;
							String dpiValue = dpi[i].value;
							
							if (dpiName != null && dpiName.toLowerCase().startsWith("version"))
							{
								version = dpiValue;
								if (version != null && version.indexOf('\n') >= 0)
									version = version.substring(0, version.indexOf('\n'));
							}
						}
					}
				}
				catch (Throwable ignore) {}

				//----------------------------------------------------
				// Add a DriverInfoEntry to the model
				DriverInfoEntry entry = new DriverInfoEntry();

				entry.setInSystemPath(inSystemPath);
				entry.setClassName(className);
				entry.setDescription(desc);
				entry.setHomePage(homePage);
				entry.setVersion(version);
				entry.setJarFile(jarFile);
				entry.setToString(driver.toString());
				entry.setUrlTemplateList(templates);

				addEntry(entry);
			}
		}
	}


//	private class AddAction
//	extends AbstractAction
//	{
//		private static final long serialVersionUID = 1L;
//
//		private static final String NAME = "Add";
//		private static final String ICON = "images/favorite_add.png";
//
//		public AddAction()
//		{
//			super(NAME, SwingUtils.readImageIcon(Version.class, ICON));
//		}
//
//		@Override
//		public void actionPerformed(ActionEvent e)
//		{
//			DriverInfoEntry entry = AddOrChangeEntryDialog.showDialog(FavoriteCommandDialog.this, null);
//			if (entry != null)
//				_tm.addEntry(-1, entry);
//		}
//	}

	/*---------------------------------------------------
	** BEGIN: class AddOrChangeEntryDialog
	**---------------------------------------------------
	*/
	private static class AddOrChangeEntryDialog
	extends JDialog
	implements ActionListener, FocusListener
	{
		private static final long serialVersionUID = 1L;

		public        int  _dialogType     = -1;
		public static int  ADD_DIALOG      = 1;
		public static int  CHANGE_DIALOG   = 2;

		private JButton    _ok             = new JButton("OK");
		private JButton    _cancel         = new JButton("Cancel");

		private DriverInfoEntry _return = null;
		private DriverInfoEntry _entry  = null;
		
		private JLabel               _className_lbl         = new JLabel("Classname");
		private JTextField           _className_txt         = new JTextField();

		private JLabel               _description_lbl       = new JLabel("Description");
		private JTextField           _description_txt       = new JTextField();

		private JLabel               _homePage_lbl          = new JLabel("Home Page");
		private JTextField           _homePage_txt          = new JTextField();

		private JLabel               _jarFile_lbl           = new JLabel("JAR File");
		private JTextField           _jarFile_txt           = new JTextField();
		private JButton              _jarFile_but           = new JButton("...");

		private JLabel               _urlTemplateLst_lbl    = new JLabel("URL Template List");
		private DefaultListModel     _urlTemplateLst_dlm    = new DefaultListModel();
		private JList                _urlTemplateLst_lst    = new JList(_urlTemplateLst_dlm);
		private JLabel               _urlTemplate_lbl       = new JLabel("URL Template");
		private JTextField           _urlTemplate_txt       = new JTextField();
		private JButton              _urlTemplateAdd_but    = new JButton("Add");
		private JButton              _urlTemplateRemove_but = new JButton("Remove");

		private AddOrChangeEntryDialog(JDialog owner, DriverInfoEntry entry)
		{
			super(owner, "", true);

			_dialogType   = entry == null ? ADD_DIALOG : CHANGE_DIALOG;
			_entry        = entry;
			if (_entry == null)
				_entry = new DriverInfoEntry();

			initComponents();
			pack();
		}

		public static DriverInfoEntry showDialog(JDialog owner, DriverInfoEntry entry)
		{
			AddOrChangeEntryDialog dialog = new AddOrChangeEntryDialog(owner, entry);
			dialog.setLocationRelativeTo(owner);
			dialog.setFocus();
			dialog.setVisible(true);
			dialog.dispose();

			return dialog._return;
		}

		protected void initComponents() 
		{
			JPanel panel = new JPanel();
			panel.setLayout(new MigLayout("insets 20 20"));   // insets Top Left Bottom Right

			if (_dialogType == ADD_DIALOG)
			{
				setTitle("New JDBC Driver");
			}
			else if (_dialogType == CHANGE_DIALOG)
			{
				setTitle("Change JDBC Driver");
			}
			else throw new RuntimeException("Unknown Dialog Type");

			_className_lbl  .setToolTipText("<html>Classname of the JDBC Driver, for example <i>com.sybase.jdbc4.jdbc.SybDriver</i>.</html>");
			_className_txt  .setToolTipText(_className_lbl.getToolTipText());
			_description_lbl.setToolTipText("<html><i><b>Optional</b></i> A Text description of what this this JDBC Driver is. (from what company etc...)</html>");
			_description_txt.setToolTipText(_description_lbl.getToolTipText());
			_homePage_lbl   .setToolTipText("<html><i><b>Optional</b></i> A web page where you can find some info about the Driver</html>");
			_homePage_txt   .setToolTipText(_homePage_lbl.getToolTipText());
			_jarFile_lbl    .setToolTipText("<html>Name of the JAR file the Driver (above Classname) is located in.</html>");
			_jarFile_txt    .setToolTipText(_jarFile_lbl.getToolTipText());
			_jarFile_but    .setToolTipText("<html>Open a File Dialog where you can choose a JAR File.</html>");
			
			_urlTemplateLst_lbl   .setToolTipText("<html>List of Examples/Templates that could be used when connecting using this Driver.</html>");
			_urlTemplateLst_lst   .setToolTipText(_urlTemplateLst_lst.getToolTipText());
			_urlTemplate_lbl      .setToolTipText("<html><i><b>Optional</b></i> A Template which can be used when connecting using this JDBC Driver.<br>Multiple Templates can be added.</html>");
			_urlTemplate_txt      .setToolTipText(_urlTemplate_lbl.getToolTipText());
			_urlTemplateAdd_but   .setToolTipText("<html>Add <b>this</b> entry to the list of URL Templates for this Driver.</html>");
			_urlTemplateRemove_but.setToolTipText("<html>Remove <b>this/selected</b> entry to the list of URL Templates for this Driver.</html>");

			panel.add(_className_lbl,         "");
			panel.add(_className_txt,         "pushx, growx, wrap");
			
			panel.add(_description_lbl,       "");
			panel.add(_description_txt,       "pushx, growx, wrap");
			
			panel.add(_homePage_lbl,          "");
			panel.add(_homePage_txt,          "pushx, growx, wrap");
			
			panel.add(_jarFile_lbl,           "");
			panel.add(_jarFile_txt,           "split, span 2, pushx, growx");
			panel.add(_jarFile_but,           "wrap 15");

			JScrollPane scroll = new JScrollPane();
			scroll.setViewportView(_urlTemplateLst_lst);
			panel.add(_urlTemplateLst_lbl,    "");
			panel.add(scroll,                 "push, grow, wrap");

			panel.add(_urlTemplate_lbl,       "");
			panel.add(_urlTemplate_txt,       "split, span 3, pushx, growx");
			panel.add(_urlTemplateAdd_but,    "");
			panel.add(_urlTemplateRemove_but, "wrap 15");
			
			// ADD the OK, Cancel, Apply buttons
			panel.add(_ok,     "tag ok,     gap top 20, skip, split, bottom, right, pushx");
			panel.add(_cancel, "tag cancel,                   split, bottom");

			setContentPane(panel);

			// Fill in some start values
			_className_txt  .setText(_entry.getClassName());
			_description_txt.setText(_entry.getDescription());
			_homePage_txt   .setText(_entry.getHomePage());
			_jarFile_txt    .setText(_entry.getJarFile());
			for (String urlTemplate : _entry.getUrlTemplateList())
			{
//System.out.println("ADD CHANGE: add template '"+urlTemplate+"'.");
				_urlTemplateLst_dlm.addElement(urlTemplate);
			}
			
			// ADD KEY listeners

			// ADD ACTIONS TO COMPONENTS
			_jarFile_but          .addActionListener(this);
			_urlTemplate_txt      .addActionListener(this);
			_urlTemplateAdd_but   .addActionListener(this);
			_urlTemplateRemove_but.addActionListener(this);
			_ok                   .addActionListener(this);
			_cancel               .addActionListener(this);

			// ADD Focus Listeners
			_className_txt  .addFocusListener(this);
			_description_txt.addFocusListener(this);
			_homePage_txt   .addFocusListener(this);
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			Object source = e.getSource();

			// --- BUTTON: OK ---
			if (_ok.equals(source))
			{
				_entry.setClassName  (_className_txt  .getText());
				_entry.setDescription(_description_txt.getText());
				_entry.setHomePage   (_homePage_txt   .getText());
				_entry.setJarFile    (_jarFile_txt    .getText());

				ArrayList<String> urlTemplates = new ArrayList<String>();
				for (int i=0; i<_urlTemplateLst_dlm.size(); i++)
					urlTemplates.add( _urlTemplateLst_dlm.getElementAt(i) + "" );
				_entry.setUrlTemplateList(urlTemplates);

				_return = _entry;

				setVisible(false);
			}

			// --- BUTTON: CANCEL ---
			if (_cancel.equals(source))
			{
				_return = null;
				setVisible(false);
			}

			// --- BUTTON: ... ---
			if (_jarFile_but.equals(source))
			{
				String dir = _jarFile_txt.getText();
	
				JFileChooser fc = new JFileChooser(dir);
				int returnVal = fc.showOpenDialog(this);
				if(returnVal == JFileChooser.APPROVE_OPTION) 
				{
					_jarFile_txt.setText(fc.getSelectedFile().getAbsolutePath());
				}
			}

			// --- BUTTON: ADD or <return in urlTemplate_txt> ---
			if (_urlTemplateAdd_but.equals(source) || _urlTemplate_txt.equals(source))
			{
				String template = _urlTemplate_txt.getText();
				_urlTemplateLst_dlm.addElement(template);
			}

			// --- BUTTON: REMOVE ---
			if (_urlTemplateRemove_but.equals(source))
			{
				int index = _urlTemplateLst_lst.getSelectedIndex();
				if (index >= 0)
					_urlTemplateLst_dlm.remove(index);
				else
					SwingUtils.showInfoMessage(this, "Select a row", "Please select a row you want to delete");
			}
		}

		@Override
		public void focusGained(FocusEvent e)
		{
		}

		@Override
		public void focusLost(FocusEvent e)
		{
			Object source = e.getSource();

			if (_className_txt.equals(source))
			{
				String className = _className_txt.getText().trim();
				// Fill up templates
				if (_urlTemplateLst_dlm.size() == 0)
				{
					List<String> urlTemplates = getDefaultUrlTemplateList(className);
					for (String template : urlTemplates)
						_urlTemplateLst_dlm.addElement(template);
				}

				// fill description
				if (_description_txt.getText().trim().equals(""))
				{
					_description_txt.setText( getDefaultDescription(className));
				}

				// fill homepage
				if (_homePage_txt.getText().trim().equals(""))
				{
					_homePage_txt.setText( getDefaultHomePage(className));
				}
			}

			if (_description_txt.equals(source))
			{
			}

			if (_homePage_txt.equals(source))
			{
			}
		}

		/**
		 * Set focus to a good field or button
		 */
		private void setFocus()
		{
			// The components needs to be visible for the requestFocus()
			// to work, so lets the EventThreda do it for us after the windows is visible.
			Runnable deferredAction = new Runnable()
			{
				@Override
				public void run()
				{
					_className_txt.requestFocus();
				}
			};
			SwingUtilities.invokeLater(deferredAction);
		}
	}
	/*---------------------------------------------------
	** END: class AddOrChangeEntryDialog
	**---------------------------------------------------
	*/


	public static String getFileName()
	{
//		return "JdbcDriverInformation.xml";
		return _filename;
	}

	public static void setFileName(String filename)
	{
		_filename = filename;
	}

	private static void saveFile(boolean showGuiOnError)
	{
		try
		{
			saveFile(getFileName(), getModel().getEntries(false));
		}
		catch (IOException ex)
		{
			if (showGuiOnError)
				SwingUtils.showErrorMessage("writing to JDBC Driver File", "Problems writing to JDBC Driver File '"+getFileName()+"'.", ex);
		}
	}

//	private static void saveFile()
//	throws IOException
//	{
//		saveFile(getFileName(), getModel().getEntries(false));
//	}

//	private static void saveFile(ArrayList<DriverInfoEntry> list)
//	throws IOException
//	{
//		saveFile(getFileName(), list);
//	}

	/**
	 * Write a new file...
	 * @param fileName       Name of the file can be null, then we will use current file.
	 * @param list           Entries to write to the file
	 * @throws IOException   When we had problems to write.
	 */
	private static void saveFile(String fileName, ArrayList<DriverInfoEntry> list)
	throws IOException
	{
		if (StringUtil.isNullOrBlank(fileName))
			fileName = getFileName();

		try
		{
			RandomAccessFile raf = new RandomAccessFile(fileName, "rw");
			FileChannel channel = raf.getChannel();

			try 
			{
				// Get an exclusive lock on the whole file
				FileLock lock = channel.lock();

				try 
				{
					// To start of the file, truncate everything beyond position 0
					channel.truncate(0);

					// ----------------------------------------------------
					// Add Beginning of the file
					StringBuilder sb = new StringBuilder();
					sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
					sb.append("\n");
					sb.append("<!-- \n");
					sb.append("    BEGIN: Example of how a entry looks like \n");

					DriverInfoEntry dummyExample = new DriverInfoEntry();
					dummyExample.setClassName  ("com.provider.jdbc.class.name.Driver");
					dummyExample.setDescription("Short Description of the JDBC Driver Provider");
					dummyExample.setHomePage   ("www.somewhere.com/we/can/find/more/info");
					dummyExample.setJarFile    ("c:/location/where/the/diver/is/located/name.jar");
					dummyExample.addUrlTemplate("jdbc:xxx:<host>:<port>");
					dummyExample.addUrlTemplate("jdbc:xxx:another_template");
					dummyExample.addUrlTemplate("jdbc:xxx:some_other_template");
					sb.append(dummyExample.toXml());

					sb.append("    \n");
					sb.append("    END: Example of how a entry looks like\n");
					sb.append("-->\n");
					sb.append("\n");
					sb.append(XML_BEGIN_TAG_DRIVERS_LIST).append("\n");
					sb.append("\n");
					//-----------------------------------------
					// Write header
					ByteBuffer byteBuffer;
					byteBuffer = ByteBuffer.wrap(sb.toString().getBytes(Charset.forName("UTF-8")));
					channel.write(byteBuffer);


					//--------------------------------------
					// Write all the history tags in the input list
					for (DriverInfoEntry entry : list)
					{
						if (entry.getInSystemPath())
							continue;

						sb.setLength(0);
						
						sb.append(entry.toXml());
						
						byteBuffer = ByteBuffer.wrap(sb.toString().getBytes(Charset.forName("UTF-8")));
						channel.write(byteBuffer);
					}

					
					//-----------------------------------------
					// Write -end- entries
					sb.setLength(0);
					
					sb.append("\n");
					sb.append(XML_END___TAG_DRIVERS_LIST).append("\n");

					byteBuffer = ByteBuffer.wrap(sb.toString().getBytes(Charset.forName("UTF-8")));
					channel.write(byteBuffer);

					// Make sure it's written to disk.
					channel.force(true);
				}
				finally 
				{
					lock.release();
				}
			} 
			finally 
			{
				channel.close();
			}			
		}
		catch (IOException e)
		{
			_logger.warn("Problems writing to JDBC Driver File '"+fileName+"'. Caught: "+e);
			throw e;
		}
	}
	
	//---------------------------------------------------
	// Below is XML tags
	//---------------------------------------------------
	private static final String       XML_TAG_DRIVERS_LIST           = "DriversList";
	private static final String XML_BEGIN_TAG_DRIVERS_LIST           = "<"  + XML_TAG_DRIVERS_LIST + ">";
	private static final String XML_END___TAG_DRIVERS_LIST           = "</" + XML_TAG_DRIVERS_LIST + ">";
	
	private static final String       XML_TAG_DRIVER_ENTRY           = "DriverEntry";
	private static final String XML_BEGIN_TAG_DRIVER_ENTRY           = "<"  + XML_TAG_DRIVER_ENTRY + ">";
	private static final String XML_END___TAG_DRIVER_ENTRY           = "</" + XML_TAG_DRIVER_ENTRY + ">";

	private static final String       XML_SUBTAG_CLASSNAME           = "ClassName";
	private static final String XML_BEGIN_SUBTAG_CLASSNAME           = "<"  + XML_SUBTAG_CLASSNAME + ">";
	private static final String XML_END___SUBTAG_CLASSNAME           = "</" + XML_SUBTAG_CLASSNAME + ">";

	private static final String       XML_SUBTAG_DESCRIPTION         = "Description";
	private static final String XML_BEGIN_SUBTAG_DESCRIPTION         = "<"  + XML_SUBTAG_DESCRIPTION + ">";
	private static final String XML_END___SUBTAG_DESCRIPTION         = "</" + XML_SUBTAG_DESCRIPTION + ">";

	private static final String       XML_SUBTAG_HOME_PAGE           = "HomePage";
	private static final String XML_BEGIN_SUBTAG_HOME_PAGE           = "<"  + XML_SUBTAG_HOME_PAGE + ">";
	private static final String XML_END___SUBTAG_HOME_PAGE           = "</" + XML_SUBTAG_HOME_PAGE + ">";

	private static final String       XML_SUBTAG_JAR_FILE            = "JarFile";
	private static final String XML_BEGIN_SUBTAG_JAR_FILE            = "<"  + XML_SUBTAG_JAR_FILE + ">";
	private static final String XML_END___SUBTAG_JAR_FILE            = "</" + XML_SUBTAG_JAR_FILE + ">";

	private static final String       XML_SUBTAG_URL_TEMPLATE        = "UrlTemplate";
	private static final String XML_BEGIN_SUBTAG_URL_TEMPLATE        = "<"  + XML_SUBTAG_URL_TEMPLATE + ">";
	private static final String XML_END___SUBTAG_URL_TEMPLATE        = "</" + XML_SUBTAG_URL_TEMPLATE + ">";


//	   <Drivers>
//    <Driver>
//       <Name>Sybase ASE (JConnect)</Name>
//       <Identifier>jdbc:sybase:Tds</Identifier>
//       <Type>sybase-ase</Type>
//       <URLFormat>jdbc:sybase:Tds:&lt;server&gt;:&lt;port5000&gt;/&lt;database&gt;</URLFormat>
//       <WizardURLFormat>jdbc:sybase:Tds:${Server|localhost}${Port|5000||prefix=: }${Database|||prefix=/ }</WizardURLFormat>
//       <DefaultClass>com.sybase.jdbc3.jdbc.SybDriver</DefaultClass>
//       <WebSite>http://www.dbvis.com/products/dbvis/doc/supports.jsp</WebSite>
//    </Driver>
// </Drivers>

	//-------------------------------------------------------------------
	// XML PARSER
	//-------------------------------------------------------------------
	private static class JdbcDriverFileXmlParser
	extends DefaultHandler
	{
		private SAXParserFactory _saxFactory      = SAXParserFactory.newInstance();
		private SAXParser        _saxParser       = null;

		private JdbcDriverFileXmlParser()
		{
			try
			{
				_saxParser = _saxFactory.newSAXParser();
			}
			catch (SAXException e)
			{
				_logger.warn("Problems Creating JDBC Driver File XML Parser '"+getFileName()+"'. Caught: "+e, e);
			}
			catch (ParserConfigurationException e)
			{
				_logger.warn("Problems Creating JDBC Driver File XML Parser '"+getFileName()+"'. Caught: "+e, e);
			}
		}

//		public DriverInfoEntry parseEntry(String entry)
//		{
//			_lastEntry       = null;
//			_entryList       = null;
//			try
//			{
//				_saxParser.parse(new InputSource(new StringReader(entry)), this);
//			}
//			catch (SAXException e)
//			{
//				_logger.warn("Problems Parsing Command history Entry '"+entry+"'. Caught: "+e, e);
//			}
//			catch (IOException e)
//			{
//				_logger.warn("Problems Parsing Command history Entry '"+entry+"'. Caught: "+e, e);
//			}
//			return _lastEntry;
//		}

		/**
		 *  Parse a file
		 *  @param filename        Name of the file
		 *  
		 *  @return A ArrayList with DriverInfoEntry
		 */
		public ArrayList<DriverInfoEntry> parseFile(String fileName)
		{
			_lastEntry       = new DriverInfoEntry();
			_entryList       = new ArrayList<DriverInfoEntry>();

			try
			{
//				_saxParser.parse(new InputSource(new FileReader(fileName)), this);
				_saxParser.parse(new File(fileName), this);
			}
			catch (SAXException e)
			{
				_logger.warn("Problems Parsing JDBC Driver File File '"+fileName+"'. Caught: "+e, e);
			}
			catch (FileNotFoundException e)
			{
				_logger.info("The JDBC Driver File '"+fileName+"' wasn't found. Caught: "+e);
			}
			catch (IOException e)
			{
				_logger.warn("Problems Parsing JDBC Driver File '"+fileName+"'. Caught: "+e, e);
			}
			return _entryList;
		}

		//----------------------------------------------------------
		// START: XML Parsing code
		//----------------------------------------------------------
		private StringBuilder                   _xmlTagBuffer = new StringBuilder();
		private DriverInfoEntry            _lastEntry    = new DriverInfoEntry();
		private ArrayList<DriverInfoEntry> _entryList    = new ArrayList<DriverInfoEntry>();

		@Override
		public void characters(char[] buffer, int start, int length)
		{
			_xmlTagBuffer.append(buffer, start, length);
//			System.out.println("XML.character: start="+start+", length="+length+", _xmlTagBuffer="+_xmlTagBuffer);
		}

		@Override
		public void startElement(String uri, String localName, String qName, org.xml.sax.Attributes attributes) 
		throws SAXException
		{
			_xmlTagBuffer.setLength(0);
//			System.out.println("SAX: startElement: qName='"+qName+"', attributes="+attributes);
			if (XML_TAG_DRIVER_ENTRY.equals(qName))
			{
				_lastEntry = new DriverInfoEntry();
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) 
		throws SAXException
		{
//			System.out.println("SAX: endElement: qName='"+qName+"', _xmlTagBuffer="+_xmlTagBuffer);
			if (XML_TAG_DRIVER_ENTRY.equals(qName))
			{
				if (_entryList == null)
					_entryList = new ArrayList<DriverInfoEntry>();
				_entryList.add(_lastEntry);
			}
			else
			{
				if      (XML_SUBTAG_CLASSNAME   .equals(qName)) _lastEntry.setClassName  (_xmlTagBuffer.toString().trim());
				else if (XML_SUBTAG_DESCRIPTION .equals(qName)) _lastEntry.setDescription(_xmlTagBuffer.toString().trim());
				else if (XML_SUBTAG_HOME_PAGE   .equals(qName)) _lastEntry.setHomePage   (_xmlTagBuffer.toString().trim());
				else if (XML_SUBTAG_JAR_FILE    .equals(qName)) _lastEntry.setJarFile    (_xmlTagBuffer.toString().trim());
				else if (XML_SUBTAG_URL_TEMPLATE.equals(qName)) _lastEntry.addUrlTemplate(_xmlTagBuffer.toString().trim());
			}
			_xmlTagBuffer.setLength(0);
		}
		//----------------------------------------------------------
		// END: XML Parsing code
		//----------------------------------------------------------
	}


//	public static void main(String[] args)
//	{
//		printDrivers();
//		
//		Test test = new Test();
//		try
//		{
//			System.out.println("---willNotWork");
//			test.willNotWork();
//			System.out.println("---willNotWork");
//		}
//		catch (Exception e)
//		{
//			e.printStackTrace();
//		}
//
//		
//		printDrivers();
//
//		try
//		{
//			System.out.println("---willWork");
//			test.willWork();
//			System.out.println("---willWork");
//		}
//		catch (Exception e)
//		{
//			e.printStackTrace();
//		}
//
//		printDrivers();
//	}
}
