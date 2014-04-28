package com.asetune.gui;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.asetune.Version;
import com.asetune.gui.ConnectionProfile.JdbcEntry;
import com.asetune.gui.ConnectionProfile.OfflineEntry;
import com.asetune.gui.ConnectionProfile.TdsEntry;
import com.asetune.utils.AseConnectionFactory;
import com.asetune.utils.Configuration;
import com.asetune.utils.DbUtils;
import com.asetune.utils.FileUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;
import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

public class ConnectionProfileManager
{
	private static Logger _logger = Logger.getLogger(ConnectionProfileManager.class);
//	private static final long serialVersionUID = 1L;

	/** */
	private LinkedHashMap<String, ConnectionProfile> _profileMap = new LinkedHashMap<String, ConnectionProfile>();

	/** Singleton */
	private static ConnectionProfileManager _instance = null;

	/** default file name where the connection profiles are stored */
	private final static String DEFAULT_STORAGE_FILE = Version.APP_STORE_DIR + File.separator + "ConnectionProfiles.xml";

	public static final String   PROPKEY_ifile_serverAdd_doNotAskAgain    = "ConnectionProfileManager.ifile.serverAdd.doNotAskAgain";
	public static final boolean  DEFAULT_ifile_serverAdd_doNotAskAgain    = false;

	public static final String   PROPKEY_ifile_copyReadOnly_doNotAskAgain = "ConnectionProfileManager.ifile.copyReadOnly.doNotAskAgain";
	public static final boolean  DEFAULT_ifile_copyReadOnly_doNotAskAgain = false;

	
	public enum SrvType 
	{ 
		TDS_ASE, 
		TDS_ASA, 
		TDS_IQ, 
		TDS_RS, 
		TDS_OTHER, 

		JDBC_HANA,
		JDBC_H2,
		JDBC_HSQL,
		JDBC_MSSQL,
		JDBC_ORACLE,
		JDBC_DB2_UX,
		JDBC_DB2_ZOS,
		JDBC_MYSQL,
		JDBC_DERBY,
		JDBC_OTHER;
	}

	// Sybase products
	public static final ImageIcon ICON_DB_PROD_NAME_SYBASE_ASE   = SwingUtils.readImageIcon(Version.class, "images/conn_profile_ase_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_SYBASE_ASA   = SwingUtils.readImageIcon(Version.class, "images/conn_profile_asa_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_SYBASE_IQ    = SwingUtils.readImageIcon(Version.class, "images/conn_profile_iq_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_SYBASE_RS    = SwingUtils.readImageIcon(Version.class, "images/conn_profile_rs_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_SYBASE_OTHER = SwingUtils.readImageIcon(Version.class, "images/conn_profile_tds_other_16.png");

	// SAP products
	public static final ImageIcon ICON_DB_PROD_NAME_HANA         = SwingUtils.readImageIcon(Version.class, "images/conn_profile_hana_16.png");

	// JDBC products
	public static final ImageIcon ICON_DB_PROD_NAME_H2           = SwingUtils.readImageIcon(Version.class, "images/conn_profile_h2_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_HSQL         = SwingUtils.readImageIcon(Version.class, "images/conn_profile_hsql_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_MSSQL        = SwingUtils.readImageIcon(Version.class, "images/conn_profile_mssql_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_ORACLE       = SwingUtils.readImageIcon(Version.class, "images/conn_profile_oracle_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_DB2_UX       = SwingUtils.readImageIcon(Version.class, "images/conn_profile_db2_ux_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_DB2_ZOS      = SwingUtils.readImageIcon(Version.class, "images/conn_profile_db2_zos_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_MYSQL        = SwingUtils.readImageIcon(Version.class, "images/conn_profile_mysql_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_DERBY        = SwingUtils.readImageIcon(Version.class, "images/conn_profile_derby_16.png");

	public static final ImageIcon ICON_DB_PROD_NAME_OTHER        = SwingUtils.readImageIcon(Version.class, "images/conn_profile_unknown_vendor_16.png");
	
	
	//---------------------------------------------------------------
	// BEGIN: Constructors
	//---------------------------------------------------------------
	public ConnectionProfileManager(String file)
	{
	}
	//---------------------------------------------------------------
	// END: Constructors
	//---------------------------------------------------------------

	//---------------------------------------------------------------
	// BEGIN: Singleton
	//---------------------------------------------------------------
	public static ConnectionProfileManager getInstance()
	{
		if ( _instance == null )
		{
			_logger.debug("No Connection Profile has yet been initiated, creating one now using file '"+DEFAULT_STORAGE_FILE+"'.");
			_instance = new ConnectionProfileManager(DEFAULT_STORAGE_FILE);
		}
		return _instance;
	}

	public static boolean hasInstance(String confName)
	{
		return _instance != null;
	}

	public static void setInstance(ConnectionProfileManager connProfile)
	{
		_instance = connProfile;
	}
	//---------------------------------------------------------------
	// END: Singleton
	//---------------------------------------------------------------

	/**
	 * A ConnectionProfile object, associated to the name
	 * @param name name of the profile we want to get
	 * @return A connection object, if not found null will be returned
	 */
	public ConnectionProfile getProfile(String name)
	{
		return _profileMap.get(name);
	}
	
	/**
	 * Get all saved ConnectionProfile objects
	 * @return a LinkedHasMap of <name,ConnectionProfile> (will never be null, if none found it will be an empty list)
	 */
	public LinkedHashMap<String, ConnectionProfile> getProfiles()
	{
		return _profileMap;
	}

	/**
	 * Get saved Connection Profiles
	 * @return a Set of Strings (will never be null, if none found it will be an empty list)
	 */
	public Set<String> getProfileNames()
	{
		return _profileMap.keySet();
	}

	public static ImageIcon getIcon(String productName)
	{
		if      (DbUtils.DB_PROD_NAME_DB2_UX    .equals(productName)) return ICON_DB_PROD_NAME_DB2_UX;
		else if (DbUtils.DB_PROD_NAME_DB2_ZOS   .equals(productName)) return ICON_DB_PROD_NAME_DB2_ZOS;
		else if (DbUtils.DB_PROD_NAME_DERBY     .equals(productName)) return ICON_DB_PROD_NAME_DERBY;
		else if (DbUtils.DB_PROD_NAME_H2        .equals(productName)) return ICON_DB_PROD_NAME_H2;
		else if (DbUtils.DB_PROD_NAME_HANA      .equals(productName)) return ICON_DB_PROD_NAME_HANA;
		else if (DbUtils.DB_PROD_NAME_HSQL      .equals(productName)) return ICON_DB_PROD_NAME_HSQL;
		else if (DbUtils.DB_PROD_NAME_MSSQL     .equals(productName)) return ICON_DB_PROD_NAME_MSSQL;
		else if (DbUtils.DB_PROD_NAME_MYSQL     .equals(productName)) return ICON_DB_PROD_NAME_MYSQL;
		else if (DbUtils.DB_PROD_NAME_ORACLE    .equals(productName)) return ICON_DB_PROD_NAME_ORACLE;
		
		else if (DbUtils.DB_PROD_NAME_SYBASE_ASA.equals(productName)) return ICON_DB_PROD_NAME_SYBASE_ASA;
		else if (DbUtils.DB_PROD_NAME_SYBASE_ASE.equals(productName)) return ICON_DB_PROD_NAME_SYBASE_ASE;
		else if (DbUtils.DB_PROD_NAME_SYBASE_IQ .equals(productName)) return ICON_DB_PROD_NAME_SYBASE_IQ;
		else if (DbUtils.DB_PROD_NAME_SYBASE_RS .equals(productName)) return ICON_DB_PROD_NAME_SYBASE_RS;

		return ICON_DB_PROD_NAME_OTHER;
	}
	public static ImageIcon getIcon(SrvType srvType)
	{
		if      (SrvType.JDBC_DB2_UX .equals(srvType)) return ICON_DB_PROD_NAME_DB2_UX;
		else if (SrvType.JDBC_DB2_ZOS.equals(srvType)) return ICON_DB_PROD_NAME_DB2_ZOS;
		else if (SrvType.JDBC_DERBY  .equals(srvType)) return ICON_DB_PROD_NAME_DERBY;
		else if (SrvType.JDBC_H2     .equals(srvType)) return ICON_DB_PROD_NAME_H2;
		else if (SrvType.JDBC_HANA   .equals(srvType)) return ICON_DB_PROD_NAME_HANA;
		else if (SrvType.JDBC_HSQL   .equals(srvType)) return ICON_DB_PROD_NAME_HSQL;
		else if (SrvType.JDBC_MSSQL  .equals(srvType)) return ICON_DB_PROD_NAME_MSSQL;
		else if (SrvType.JDBC_MYSQL  .equals(srvType)) return ICON_DB_PROD_NAME_MYSQL;
		else if (SrvType.JDBC_ORACLE .equals(srvType)) return ICON_DB_PROD_NAME_ORACLE;

		else if (SrvType.TDS_ASA     .equals(srvType)) return ICON_DB_PROD_NAME_SYBASE_ASA;
		else if (SrvType.TDS_ASE     .equals(srvType)) return ICON_DB_PROD_NAME_SYBASE_ASE;
		else if (SrvType.TDS_IQ      .equals(srvType)) return ICON_DB_PROD_NAME_SYBASE_IQ;
		else if (SrvType.TDS_RS      .equals(srvType)) return ICON_DB_PROD_NAME_SYBASE_RS;
		else if (SrvType.TDS_OTHER   .equals(srvType)) return ICON_DB_PROD_NAME_SYBASE_OTHER;

		return ICON_DB_PROD_NAME_OTHER;
	}

	public void possiblyAddChange(String key, String productName, TdsEntry tds)
	{
		possiblyAddChange(key, productName, tds, null, null);
		checkAddEntryToInterfacesFile(tds._tdsIfile, key);
	}

	public void possiblyAddChange(String key, String productName, OfflineEntry offline)
	{
		possiblyAddChange(key, productName, null, offline, null);
	}

	public void possiblyAddChange(String key, String productName, JdbcEntry jdbc)
	{
		possiblyAddChange(key, productName, null, null, jdbc);
	}

	private void possiblyAddChange(String key, String productName, TdsEntry tds, OfflineEntry offline, JdbcEntry jdbc)
	{
		// Check if the profile exists
		// If the profile doesn't exists, ask if we should create a new one, AND Give it a PROPER NAME
		
		// If the profile has been updated, ask if we want to update the information in the ConnectionProfile as well
		
	}
	
	
	private void checkAddEntryToInterfacesFile(String sqlIniFileName, String hostPortStr)
	{
		// Server name was NOT found in the interfaces file
		// Ask if you want to add it to the interfaces file
		String server = AseConnectionFactory.getIServerName(hostPortStr);
		
		boolean doNotAskAgain = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_ifile_serverAdd_doNotAskAgain, DEFAULT_ifile_serverAdd_doNotAskAgain);
		if (StringUtil.isNullOrBlank(server) && doNotAskAgain==false)
		{
			// Check if the sql.ini / interfaces is writable... before we edit the file...
			// if it's read only - maybe ask if we should copy the file and add the entry...
			String htmlMsg = "<html>" +
					"<h3>Add server to sql.ini or interfaces file</h3>" +
					"The server name was <b>not</b> found in file <code>" + sqlIniFileName + "</code><br>" +
					"Do you want to add it to the interfaces file?" +
					"</html>";

			// Compose a possible server name
			String suggestedName = "";
			Map<String, String> hostPortMap = StringUtil.parseCommaStrToMap(hostPortStr, ":", ",");
			for (String key : hostPortMap.keySet())
			{
				String val = hostPortMap.get(key);

				// If first entry is localhost, get next entry
				if (key.equalsIgnoreCase("localhost") && hostPortMap.size() > 1)
					continue;

				suggestedName = key.toUpperCase();
				if (suggestedName.indexOf(".") > 0) // hostname.acme.com
					suggestedName = suggestedName.substring(0, suggestedName.indexOf("."));
				suggestedName += "_" + val;

				break; // only get first entry
			}
			
			JLabel     header    = new JLabel(htmlMsg);
			JLabel     label     = new JLabel("As Server Name");
			JTextField entryName = new JTextField(suggestedName, 30);
			JPanel     panel     = new JPanel(new MigLayout());
			panel.add(header,    "span, wrap 20");
			panel.add(label,     "");
			panel.add(entryName, "wrap 20");

			String[] options = { "Add Server", "Not this time", "Never ask this question again" };
			int result = JOptionPane.showOptionDialog(null, panel, "Add Server Name", 
					JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, 0);

			if (result == 0) // save
			{
				boolean canWrite = FileUtils.canWrite(sqlIniFileName);
				if (canWrite)
					AseConnectionFactory.addIFileEntry(sqlIniFileName, entryName.getText(), hostPortStr);
				else
				{
//					String htmlStr = "<html>" +
//							"<h3>Warning</h3>" +
//							"Name service file '"+sqlIniFileName+"' is <b>not writable</b>" +
//							"So adding/changing entries will be impossible!<br>" +
//							"</html>";
//					SwingUtils.showWarnMessage(null, "not writable", htmlStr, null);

					// Copy the file to $HOME/.asetune/sql.ini and add it there???
					String newFile = copyInterfacesFileToPrivateFile(sqlIniFileName);
					if (newFile != null)
						AseConnectionFactory.addIFileEntry(newFile, entryName.getText(), hostPortStr);
				}
			}
			if (result == 2) // never
			{
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf != null)
				{
					conf.setProperty(PROPKEY_ifile_serverAdd_doNotAskAgain, true);
					conf.save();
				}
			}
		}
	}
	
	public String copyInterfacesFileToPrivateFile(String currentInterfacesFile)
	{
		String newFileName = null;

		String privateSqlIni = AseConnectionFactory.getPrivateInterfacesFile(false);
		
		// If load file and private is not the same file AND "load" is read only
		// Should we copy the "source" file to the "private" file 
		if ( ! privateSqlIni.equalsIgnoreCase(currentInterfacesFile) )
		{
			boolean doNotAskAgain = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_ifile_copyReadOnly_doNotAskAgain, DEFAULT_ifile_copyReadOnly_doNotAskAgain);
			if (doNotAskAgain == false)
			{
				String htmlMsg = "<html>" +
					"<h3>Warning</h3>" +
					"Name service file <code>"+currentInterfacesFile+"</code> is <b>not writable</b><br>" +
					"So adding/changing entries will be impossible!<br>" +
					"<br>" +
					"If you copy the file to a <i>private</i> file, then you can add and maintain you'r own entries in that file.<br>" +
					"<br>" +
					"<br>" +
					"Do you want to copy the file?<br>" +
					"<ul>" +
					"   <li>from: <code>" + currentInterfacesFile + "</code></li>" +
					"   <li>to:   <code>" + privateSqlIni         + "</code></li>" +
					"</ul>" +
					"</html>";

				String[] options = { "Copy", "Not this time", "Never ask this question again" };
				int result = JOptionPane.showOptionDialog(null, htmlMsg, "Copy File?", 
						JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, 0);
				
				if (result == 0) // Copy
				{
					try
					{
						FileUtils.copy(currentInterfacesFile, privateSqlIni, true, true); // Overwrite of any existing file will have to be confirmed, using GUI question
						
						// lets continue with the name in privateSqlIni
						newFileName = privateSqlIni;

						// Save the new interfaces file name in the properties
						// which will picked up by ConnectionDialog next time it opens
						Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
						if (conf != null)
						{
							conf.setProperty("conn.interfaces", newFileName);
							conf.save();
						}

						// Simply sets System.setProperty("sybase.home", toTheFileName)
						AseConnectionFactory.getPrivateInterfacesFile(true); 
					}
					catch (IOException ex)
					{
    					htmlMsg = "<html>" +
        						"<h3>Problems Copy the File</h3>" +
        						"Sorry, There were problems when trying to copy the file.<br>" +
        						"<ul>" +
        						"   <li>from: <code>" + currentInterfacesFile + "</code></li>" +
        						"   <li>to:   <code>" + privateSqlIni         + "</code></li>" +
        						"</ul>" +
        						"So lets <b>continue to use the file <code>"+currentInterfacesFile+"</code></b><br>" +
        						"</html>";
						SwingUtils.showWarnMessage(null, "Copy File: Problems", htmlMsg, ex);
					}
				}
				if (result == 2) // Never ask this question again
				{
					Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
					if (conf != null)
					{
						conf.setProperty(PROPKEY_ifile_copyReadOnly_doNotAskAgain, true);
						conf.save();
					}
				}
	    	} // end: doNotAskAgain == false
		} // end:  privateSqlIni != file
    	
    	return newFileName;
	}

































	/**
	 * 
	 */
	public void save()
	{
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		try
		{
			dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.newDocument();
			// add elements to Document
			Element rootElement = doc.createElementNS("http://www.journaldev.com/employee", "Employees");
			// append root element to document
			doc.appendChild(rootElement);

			// append first child element to root element
			rootElement.appendChild(getEmployee(doc, "1", "Pankaj", "29", "Java Developer", "Male"));

			// append second child
			rootElement.appendChild(getEmployee(doc, "2", "Lisa", "35", "Manager", "Female"));

			// for output to file, console
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			// for pretty print
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			DOMSource source = new DOMSource(doc);

			// write to console or file
			StreamResult console = new StreamResult(System.out);
			StreamResult file = new StreamResult(new File("/Users/pankaj/emps.xml"));

			// write data
			transformer.transform(source, console);
			transformer.transform(source, file);
			System.out.println("DONE");

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private Node getEmployee(Document doc, String id, String name, String age, String role, String gender)
	{
		Element employee = doc.createElement("Employee");

		// set id attribute
		employee.setAttribute("id", id);

		// create name element
		employee.appendChild(getEmployeeElements(doc, employee, "name", name));

		// create age element
		employee.appendChild(getEmployeeElements(doc, employee, "age", age));

		// create role element
		employee.appendChild(getEmployeeElements(doc, employee, "role", role));

		// create gender element
		employee.appendChild(getEmployeeElements(doc, employee, "gender", gender));

		return employee;
	}

	// utility method to create text node
	private Node getEmployeeElements(Document doc, Element element, String name, String value)
	{
		Element node = doc.createElement(name);
		node.appendChild(doc.createTextNode(value));
		return node;
	}

	/**
	 * @param unformattedXml - XML String
	 * @return Properly formatted XML String
	 */
	public String format(String unformattedXml)
	{
		try
		{
			Document document = parseXmlFile(unformattedXml);

			OutputFormat format = new OutputFormat(document);
			format.setLineWidth(65);
			format.setIndenting(true);
			format.setIndent(2);
			Writer out = new StringWriter();
			XMLSerializer serializer = new XMLSerializer(out, format);
			serializer.serialize(document);

			return out.toString();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return "";
		}

	}

	/**
	 * This function converts String XML to Document object
	 * 
	 * @param in - XML String
	 * @return Document object
	 */
	private Document parseXmlFile(String in)
	{
		try
		{
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			InputSource is = new InputSource(new StringReader(in));
			return db.parse(is);
		}
		catch (ParserConfigurationException e)
		{
			throw new RuntimeException(e);
		}
		catch (SAXException e)
		{
			throw new RuntimeException(e);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Takes an XML Document object and makes an XML String. Just a utility function.
	 * 
	 * @param doc - The DOM document
	 * @return the XML String
	 */
	public String makeXMLString(Document doc)
	{
		String xmlString = "";
		if ( doc != null )
		{
			try
			{
				TransformerFactory transfac = TransformerFactory.newInstance();
				Transformer trans = transfac.newTransformer();
				trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
				trans.setOutputProperty(OutputKeys.INDENT, "yes");
				StringWriter sw = new StringWriter();
				StreamResult result = new StreamResult(sw);
				DOMSource source = new DOMSource(doc);
				trans.transform(source, result);
				xmlString = sw.toString();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		return xmlString;
	}

	public static void main(String args[])
	{
		ConnectionProfileManager formatter = new ConnectionProfileManager("");
		String book = "<?xml version=\"1.0\"?><catalog><book id=\"bk101\"><author>Gambardella, Matthew</author><title>XML Developers Guide</title><genre>Computer</genre><price>44.95</price><publish_date>2000-10-01</publish_date><description>An in-depth look at creating applications with XML.</description></book><book id=\"bk102\"><author>Ralls, Kim</author><title>Midnight Rain</title><genre>Fantasy</genre><price>5.95</price><publish_date>2000-12-16</publish_date><description>A former architect battles corporate zombies, an evil sorceress, and her own childhood to become queen of the world.</description></book></catalog>";
		System.out.println(formatter.format(book));
	}

}
