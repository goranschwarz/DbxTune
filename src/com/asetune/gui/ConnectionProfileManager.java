package com.asetune.gui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.asetune.Version;
import com.asetune.gui.ConnectionProfile.ConnProfileEntry;
import com.asetune.gui.ConnectionProfile.JdbcEntry;
import com.asetune.gui.ConnectionProfile.OfflineEntry;
import com.asetune.gui.ConnectionProfile.SrvType;
import com.asetune.gui.ConnectionProfile.TdsEntry;
import com.asetune.utils.AseConnectionFactory;
import com.asetune.utils.Configuration;
import com.asetune.utils.DbUtils;
import com.asetune.utils.FileUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;

public class ConnectionProfileManager
{
	private static Logger _logger = Logger.getLogger(ConnectionProfileManager.class);
//	private static final long serialVersionUID = 1L;

	/** all connection profiles, by key: getName() from the profile */
	private LinkedHashMap<String, ConnectionProfile> _profileMap = new LinkedHashMap<String, ConnectionProfile>();

	/** all connection profiles, by key: getKey() from  the profile */
//	private LinkedHashMap<String, ConnectionProfile> _keyMap     = new LinkedHashMap<String, ConnectionProfile>();

	/** root node of the Connection Profile Tree */ 
	private DefaultMutableTreeNode _profileTreeRoot  = new DefaultMutableTreeNode("All", true);
	/** data model for the Connection Profile Tree */ 
	private FilteredTreeModel      _profileTreeModel = new FilteredTreeModel(_profileTreeRoot);


	/** Singleton */
	private static ConnectionProfileManager _instance = null;

	/** Filename holding the configuration */
	private String _filename = null;

	/** default file name where the connection profiles are stored */
	public  final static String  PROPKEY_STORAGE_FILE = "ConnectionProfileManager.storage.filename";
	public  final static String  DEFAULT_STORAGE_FILE = Version.APP_STORE_DIR + File.separator + "ConnectionProfiles.xml";

	public static final String   PROPKEY_connProfile_serverAdd_showDialog = "ConnectionProfileManager.connProfile.serverAdd.showDialog";
	public static final boolean  DEFAULT_connProfile_serverAdd_showDialog = true;

	public static final String   PROPKEY_connProfile_changed_showDialog   = "ConnectionProfileManager.connProfile.changed.showDialog";
	public static final boolean  DEFAULT_connProfile_changed_showDialog   = true;

	public static final String   PROPKEY_connProfile_changed_alwaysSave   = "ConnectionProfileManager.connProfile.changed.alwaysSave";
	public static final boolean  DEFAULT_connProfile_changed_alwaysSave   = true;

	public static final String   PROPKEY_ifile_serverAdd_showDialog       = "ConnectionProfileManager.ifile.serverAdd.showDialog";
	public static final boolean  DEFAULT_ifile_serverAdd_showDialog       = true;

	public static final String   PROPKEY_ifile_copyReadOnly_showDialog    = "ConnectionProfileManager.ifile.copyReadOnly.showDialog";
	public static final boolean  DEFAULT_ifile_copyReadOnly_showDialog    = true;

	public static final String   PROPKEY_connProfile_load_onNodeSelection = "ConnectionProfileManager.connProfile.load.onNodeSelection";
	public static final boolean  DEFAULT_connProfile_load_onNodeSelection = true;

	
	// Sybase products
	public static final ImageIcon ICON_DB_PROD_NAME_SYBASE_ASE       = SwingUtils.readImageIcon(Version.class, "images/conn_profile_ase_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_SYBASE_ASA       = SwingUtils.readImageIcon(Version.class, "images/conn_profile_asa_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_SYBASE_IQ        = SwingUtils.readImageIcon(Version.class, "images/conn_profile_iq_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_SYBASE_RS        = SwingUtils.readImageIcon(Version.class, "images/conn_profile_rs_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_SYBASE_OTHER     = SwingUtils.readImageIcon(Version.class, "images/conn_profile_tds_other_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_SYBASE_UNDEFINED = SwingUtils.readImageIcon(Version.class, "images/conn_profile_tds_undefined_16.png");

	// SAP products
	public static final ImageIcon ICON_DB_PROD_NAME_HANA             = SwingUtils.readImageIcon(Version.class, "images/conn_profile_hana_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_MAXDB            = SwingUtils.readImageIcon(Version.class, "images/conn_profile_maxdb_16.png");

	// JDBC products
	public static final ImageIcon ICON_DB_PROD_NAME_H2               = SwingUtils.readImageIcon(Version.class, "images/conn_profile_h2_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_HSQL             = SwingUtils.readImageIcon(Version.class, "images/conn_profile_hsql_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_MSSQL            = SwingUtils.readImageIcon(Version.class, "images/conn_profile_mssql_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_ORACLE           = SwingUtils.readImageIcon(Version.class, "images/conn_profile_oracle_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_DB2_UX           = SwingUtils.readImageIcon(Version.class, "images/conn_profile_db2_ux_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_DB2_ZOS          = SwingUtils.readImageIcon(Version.class, "images/conn_profile_db2_zos_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_MYSQL            = SwingUtils.readImageIcon(Version.class, "images/conn_profile_mysql_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_DERBY            = SwingUtils.readImageIcon(Version.class, "images/conn_profile_derby_16.png");

	public static final ImageIcon ICON_DB_PROD_NAME_OTHER            = SwingUtils.readImageIcon(Version.class, "images/conn_profile_unknown_vendor_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_UNDEFINED        = SwingUtils.readImageIcon(Version.class, "images/conn_profile_jdbc_undefined_16.png");

	// XML Strings for the Catalog and ConnProfile Tree
	private final static String XML_PROFILE_TREE                     = "ProfileTree";
	private final static String XML_PROFILE_TREE_CATALOG             = "Catalog";
	private final static String XML_PROFILE_TREE_CATALOG_ATTR_NAME   = "name";
	private final static String XML_PROFILE_TREE_ENTRY               = "Entry";
	private final static String XML_PROFILE_TREE_ENTRY_ATTR_NAME     = "name";

	
	//---------------------------------------------------------------
	// BEGIN: Constructors
	//---------------------------------------------------------------
	public ConnectionProfileManager(String filename)
	{
		setFilename(filename);
	}
	//---------------------------------------------------------------
	// END: Constructors
	//---------------------------------------------------------------

	//---------------------------------------------------------------
	// BEGIN: Singleton
	//---------------------------------------------------------------
	/**
	 * Get the singleton, if one dosn't exists, create one with the default profile xml file 
	 */
	public static ConnectionProfileManager getInstance()
	{
		if ( _instance == null )
		{
			String filename = Configuration.getCombinedConfiguration().getProperty(PROPKEY_STORAGE_FILE, DEFAULT_STORAGE_FILE);
			_logger.debug("No Connection Profile has yet been initiated, creating one now using file '"+filename+"'.");
			_instance = new ConnectionProfileManager(filename);
		}
		return _instance;
	}

	/**
	 * Checks if a connection profile has been created
	 */
	public static boolean hasInstance()
	{
		return _instance != null;
	}

	/**
	 * Set a instance of the connection profile to be used as the singleton
	 */
	public static void setInstance(ConnectionProfileManager connProfile)
	{
		_instance = connProfile;
	}
	//---------------------------------------------------------------
	// END: Singleton
	//---------------------------------------------------------------

	/**
	 * Get the file name of currently used profile file
	 */
	public String getFilename()
	{
		return _filename;
	}

	/**
	 * Set a file keeping entries and parse the file
	 * @param filename
	 */
	public void setFilename(String filename)
	{
		if (StringUtil.isNullOrBlank(filename))
			filename = Configuration.getCombinedConfiguration().getProperty(PROPKEY_STORAGE_FILE, DEFAULT_STORAGE_FILE);

		// If the file doesn't exists
		File f = new File(filename);
		if ( ! f.exists() )
		{
			if (filename.equals(DEFAULT_STORAGE_FILE))
			{
				createStorageFile(filename);
			}
			else
			{
				String htmlMsg = 
						"<html>"
						+ "The Connection Dialog storage file '"+filename+"' doesn't exist!<br>"
						+ "<br>"
						+ "Do you want to create the above file and start to use that?"
						+ "</html>";

				int answer = JOptionPane.showConfirmDialog(null, htmlMsg, "File dosn't exist", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
				if ( answer == JOptionPane.YES_OPTION )
				{
					createStorageFile(filename);
				}
				else
				{
					RuntimeException ex = new RuntimeException("File '"+filename+"' not found (and 'create-new-file' was declined). Cant continue to parse the file.");
					_logger.error("File '"+filename+"' not found (and 'create-new-file' was declined). Cant continue to parse the file.", ex);
//					throw ex;
					return;
				}
			}
		}
		_logger.info("Connection Profile Manager will use the file "+filename+" to store profile information.");

		// Only set if it's a new name
//		if ( ! filename.equals(_filename) )
		if ( true )
		{
			_filename = filename;
			parseXmlFile(filename);
		}
	}

	/**
	 * A ConnectionProfile object, associated to the name
	 * <p>
	 * First it searches the Map containing profiles with key: getName()<br>
	 * Second it searches the Map containing profiles with key: getKey()<br>
	 * 
	 * @param name name of the profile we want to get
	 * @return A connection object, if not found null will be returned
	 */
	public ConnectionProfile getProfile(String name)
	{
		if (StringUtil.isNullOrBlank(name))
			return null;

		ConnectionProfile connProfile = _profileMap.get(name);

		// If not found in the ProfilesMap, check the KEY Map
//		if (connProfile == null)
//		connProfile = _keyMap.get(name);
		if (connProfile == null)
		{
			List<ConnectionProfile> profiles = getProfileByKey(name);
			if (profiles.size() > 0)
				connProfile = profiles.get(0);
		}

		return connProfile;
	}

	/**
	 * Get a ConnectionProfile by it's key
	 * @param hostPortStr
	 */
	public List<ConnectionProfile> getProfileByKey(String key)
	{
		ArrayList<ConnectionProfile> list = new ArrayList<ConnectionProfile>();
		for (ConnectionProfile cp : _profileMap.values())
		{
			if (key.equals(cp.getKey()))
				list.add(cp);
		}
		return list;
	}

	
	/**
	 * Add a profile to the manager
	 * 
	 * @param connProfile the profile to add
	 * @param addToTree true if we want to add the profile to the Tree as well
	 */
	public void addProfile(ConnectionProfile connProfile, boolean addToTree)
	{
		_profileMap.put(connProfile.getName(), connProfile);
//		_keyMap    .put(connProfile.getKey(),  connProfile);
		
		if (addToTree)
		{
			// Add it to the tree if not already there
//			if ( getTreePath(connProfile) == null )
//				_profileTreeRoot.add(new DefaultMutableTreeNode(connProfile));
			if ( getTreePath(connProfile) == null )
			{
				_profileTreeModel.insertNodeInto(
						new DefaultMutableTreeNode(connProfile), 
						_profileTreeRoot, 
						_profileTreeRoot.getChildCount());
			}
		}
//		save();
	}

	/**
	 * Set a new ConnectionProfile for an already existing Connection Profile to the manager
	 * 
	 * @param connProfile the profile to add
	 * @param connProfileEntry 
	 * @param addToTree true if we want to add the profile to the Tree as well
	 */
	public void setProfileEntry(ConnectionProfile connProfile, ConnProfileEntry connProfileEntry)
	{
		connProfile.setEntry(connProfileEntry);

		_profileMap.put(connProfile.getName(), connProfile);
//		_keyMap    .put(connProfile.getKey(),  connProfile);
		
		// Add it to the tree if not already there
		// Else update the tree node
		TreeNode node[] = getTreePath(connProfile);
		if ( getTreePath(connProfile) == null )
		{
			_profileTreeModel.insertNodeInto(
					new DefaultMutableTreeNode(connProfile), 
					_profileTreeRoot, 
					_profileTreeRoot.getChildCount());
		}
		else
		{
			_profileTreeModel.nodeChanged(node[node.length-1]);
		}

//		save();
	}

	/**
	 * Delete a profile from the Connection Profiles
	 * 
	 * @param connProfile
	 */
	public void deleteProfile(ConnectionProfile connProfile)
	{
		// delete it from the maps
		_profileMap.remove(connProfile.getName());
//		_keyMap    .remove(connProfile.getKey());

		// delete it from the Tree
		TreeNode[] path = getTreePath(connProfile);
		if (path != null)
		{
			TreeNode thisNode = path[path.length-1];
			if (thisNode instanceof DefaultMutableTreeNode)
				_profileTreeModel.removeNodeFromParent((DefaultMutableTreeNode)thisNode);
		}
//		save();
	}
	
	/**
	 * Add a catalog, it will only be added to the 
	 * @param connectionProfileCatalog
	 */
	public void addCatalog(ConnectionProfileCatalog connectionProfileCatalog)
	{
		_profileTreeModel.insertNodeInto(
				new DefaultMutableTreeNode(connectionProfileCatalog), 
				_profileTreeRoot, 
				_profileTreeRoot.getChildCount());
//		save();
	}

//	public void deleteCatalog(ConnectionProfileCatalog catalog)
//	{
//		TreeNode[] path = getTreePath(catalog);
//		if (path != null)
//		{
//			TreeNode thisNode = path[path.length-1];
//			if (thisNode instanceof DefaultMutableTreeNode)
//			{
//				DefaultMutableTreeNode node = (DefaultMutableTreeNode)thisNode;
//				if (node.getChildCount() == 0)
//				{
//					_profileTreeModel.removeNodeFromParent((DefaultMutableTreeNode)thisNode);
//				}
//				else
//				{
//					String htmlMsg = 
//							"<html>"
//							+ "<h3>Can't delete</h3>"
//							+ catalog.getName() + " has " +node.getChildCount() + " <i>children</i> connected to it.<br>"
//							+ "First delete or move all children."
//							+ "</html>";
//					SwingUtils.showWarnMessage("Has children", htmlMsg, null);
//				}
//			}
//		}
//		save();
//	}
	public boolean deleteCatalog(Object catalog)
	{
		TreeNode[] path = getTreePath(catalog);
		if (path == null)
			return false;

		TreeNode thisNode = path[path.length-1];
		if (thisNode instanceof DefaultMutableTreeNode)
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)thisNode;
			if (node.getChildCount() == 0)
			{
				_profileTreeModel.removeNodeFromParent((DefaultMutableTreeNode)thisNode);
			}
			else
			{
				String htmlMsg = 
						"<html>"
						+ "<h3>Can't delete</h3>"
						+ catalog + " has " +node.getChildCount() + " <i>children</i> connected to it.<br>"
						+ "First delete or move all children."
						+ "</html>";
				SwingUtils.showWarnMessage("Has children", htmlMsg, null);
				return false;
			}
		}
		save();
		return true;
	}
	
	private TreeNode[] getTreePath(Object profile)
	{
		@SuppressWarnings("unchecked")
		Enumeration<DefaultMutableTreeNode> en = _profileTreeRoot.preorderEnumeration();	
		while (en.hasMoreElements())
		{
			DefaultMutableTreeNode node = en.nextElement();

			TreeNode[] path = node.getPath();
//System.out.println((node.isLeaf() ? "  - " : "+ ") + path[path.length - 1] + " (obj="+path[path.length - 1].getClass().getName()+")");

			if (profile.toString().equals(node.toString()))
				return path;
		}
		return null;
	}
	
	/**
	 * Check if a node exists in the tree
	 * @param nodeName name of a node
	 * @return
	 */
	public boolean exists(String nodeName)
	{
		// TODO Auto-generated method stub
		return getTreePath(nodeName) != null;
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
	public Set<String> getProfileNames(ConnectionProfile.Type type)
	{
		LinkedHashSet<String> names = new LinkedHashSet<String>();
		
		for (ConnectionProfile profile : getProfiles().values())
		{
			if (profile.isType(type))
				names.add(profile.getName());
		}
		return names;
	}

	/**
	 * Get model to tree node and the catalog, Profile entries
	 * 
	 * @return
	 */
	public FilteredTreeModel getConnectionProfileTreeModel()
	{
		return getConnectionProfileTreeModel(null);
	}

	public FilteredTreeModel getConnectionProfileTreeModel(String filterOnProductName)
	{
		_profileTreeModel.setFilterOnProductName(filterOnProductName);
		return _profileTreeModel;
	}

	public void   setTreeModelFilterOnProductName(String name) 
	{ 
		_profileTreeModel.setFilterOnProductName(name);
		_profileTreeModel.reload();
	}
	public String getTreeModelFilterOnProductName()            
	{ 
		return _profileTreeModel.getFilterOnProductName(); 
	}


	public static ImageIcon getIcon(String productName)
	{
		if      (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_DB2_UX    )) return ICON_DB_PROD_NAME_DB2_UX;
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_DB2_ZOS   )) return ICON_DB_PROD_NAME_DB2_ZOS;
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_DERBY     )) return ICON_DB_PROD_NAME_DERBY;
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_H2        )) return ICON_DB_PROD_NAME_H2;
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_HANA      )) return ICON_DB_PROD_NAME_HANA;
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_MAXDB     )) return ICON_DB_PROD_NAME_MAXDB;
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_HSQL      )) return ICON_DB_PROD_NAME_HSQL;
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_MSSQL     )) return ICON_DB_PROD_NAME_MSSQL;
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_MYSQL     )) return ICON_DB_PROD_NAME_MYSQL;
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_ORACLE    )) return ICON_DB_PROD_NAME_ORACLE;
		
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_SYBASE_ASA)) return ICON_DB_PROD_NAME_SYBASE_ASA;
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_SYBASE_ASE)) return ICON_DB_PROD_NAME_SYBASE_ASE;
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_SYBASE_IQ )) return ICON_DB_PROD_NAME_SYBASE_IQ;
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_SYBASE_RS )) return ICON_DB_PROD_NAME_SYBASE_RS;

		return ICON_DB_PROD_NAME_OTHER;
	}
	public static ImageIcon getIcon(SrvType srvType)
	{
		if      (SrvType.JDBC_DB2_UX   .equals(srvType)) return ICON_DB_PROD_NAME_DB2_UX;
		else if (SrvType.JDBC_DB2_ZOS  .equals(srvType)) return ICON_DB_PROD_NAME_DB2_ZOS;
		else if (SrvType.JDBC_DERBY    .equals(srvType)) return ICON_DB_PROD_NAME_DERBY;
		else if (SrvType.JDBC_H2       .equals(srvType)) return ICON_DB_PROD_NAME_H2;
		else if (SrvType.JDBC_HANA     .equals(srvType)) return ICON_DB_PROD_NAME_HANA;
		else if (SrvType.JDBC_MAXDB    .equals(srvType)) return ICON_DB_PROD_NAME_MAXDB;
		else if (SrvType.JDBC_HSQL     .equals(srvType)) return ICON_DB_PROD_NAME_HSQL;
		else if (SrvType.JDBC_MSSQL    .equals(srvType)) return ICON_DB_PROD_NAME_MSSQL;
		else if (SrvType.JDBC_MYSQL    .equals(srvType)) return ICON_DB_PROD_NAME_MYSQL;
		else if (SrvType.JDBC_ORACLE   .equals(srvType)) return ICON_DB_PROD_NAME_ORACLE;
		else if (SrvType.JDBC_OTHER    .equals(srvType)) return ICON_DB_PROD_NAME_OTHER;
		else if (SrvType.JDBC_UNDEFINED.equals(srvType)) return ICON_DB_PROD_NAME_UNDEFINED;

		else if (SrvType.TDS_ASA       .equals(srvType)) return ICON_DB_PROD_NAME_SYBASE_ASA;
		else if (SrvType.TDS_ASE       .equals(srvType)) return ICON_DB_PROD_NAME_SYBASE_ASE;
		else if (SrvType.TDS_IQ        .equals(srvType)) return ICON_DB_PROD_NAME_SYBASE_IQ;
		else if (SrvType.TDS_RS        .equals(srvType)) return ICON_DB_PROD_NAME_SYBASE_RS;
		else if (SrvType.TDS_OTHER     .equals(srvType)) return ICON_DB_PROD_NAME_SYBASE_OTHER;
		else if (SrvType.TDS_UNDEFINED .equals(srvType)) return ICON_DB_PROD_NAME_SYBASE_UNDEFINED;

		return ICON_DB_PROD_NAME_OTHER;
	}

	/**
	 * Possible called from Connection Dialog when a successful connection has been made, this
	 * so we can add the entry to the connection profiles
	 * 
	 * @param key                  key to be used
	 * @param productName          name of the product we connected to
	 * @param dbServerName         name of the server we connected to
	 * @param connProfileEntry     object holding all the details for this profile
	 * @param selectedProfileName  currently selected profile name (could be null)
	 * @param owner                GUI Owner/Caller
	 */
	public void possiblyAddChange(String key, boolean afterSuccessfulConnect, String productName, String dbServerName, ConnProfileEntry connProfileEntry, String selectedProfileName, JDialog owner)
	{
		ConnectionProfile connProfile = getProfile(key);

		// SELECTED profile overrides profiles by KEY
		ConnectionProfile selectedProfile = getProfile(selectedProfileName);
		_logger.debug("possiblyAddChange(): key='"+key+"', productName='"+productName+"', connProfile="+connProfile+", selectedProfileName='"+selectedProfileName+"', selectedProfile="+selectedProfile);
		if (selectedProfile != null)
			connProfile = selectedProfile;

		if (connProfile == null)
		{
			connProfile = new ConnectionProfile(key, productName, connProfileEntry);

			// Set to UNKNOWN server type if we are NOT connected... when you connect it will discover the SrvType
			if ( ! afterSuccessfulConnect )
				connProfile.setSrvTypeUnknown();

			SaveAsDialog saveAs = new SaveAsDialog(owner, dbServerName, connProfile, afterSuccessfulConnect);
			saveAs.showPossibly();
		}
		else 
		{
			// if connected: Always update the server type
			if (afterSuccessfulConnect)
				connProfile.setSrvType(productName);

			// if the previously stored profile IS an AseTune Profile
			// and we are using SQLWindow, we need to copy the "hidden" fields into the passed ConnectionProfileEntry
			// otherwise we will lose that data the next time we use the entry from AseTune
			if (connProfileEntry instanceof TdsEntry && connProfile.getEntry() instanceof TdsEntry)
			{
				TdsEntry storedTdsEntry = (TdsEntry) connProfile.getEntry();
				TdsEntry newTdsEntry    = (TdsEntry) connProfileEntry;

				if (storedTdsEntry._isAseTuneParamsValid && !newTdsEntry._isAseTuneParamsValid)
					newTdsEntry.copyAseTuneParams(storedTdsEntry);
			}

			if ( ! connProfile.equals(connProfileEntry) )
			{
				saveChangesDialog(key, connProfile, connProfileEntry, dbServerName, afterSuccessfulConnect, owner);
			}
		}

		save();
	}


	
	private class SaveAsDialog
	extends JDialog
	implements ActionListener
	{
		private static final long serialVersionUID = 1L;

		private JDialog           _owner        = null;
//		private String            _inKey        = null;
		private String            _dbServerName = null;
		private ConnectionProfile _connProfile  = null;
		@SuppressWarnings("unused")
		private boolean           _afterSuccessfulConnect = false;

		Vector<String>       _interfacesSuggestions = new Vector<String>();
		private JPanel       _interfacesName_pan    = null;
		private JLabel       _interfacesName_head   = new JLabel();
		private JLabel       _interfacesName_lbl    = new JLabel("As Server Name");
		private JComboBox    _interfacesName_cbx    = new JComboBox(_interfacesSuggestions);
		private JRadioButton _interfacesQ1_rbt      = new JRadioButton("Add Server", true);
		private JRadioButton _interfacesQ2_rbt      = new JRadioButton("Not this time");
		private JRadioButton _interfacesQ3_rbt      = new JRadioButton("Never ask this question again");
		private JLabel       _interfacesName_bussy  = new JLabel("The selected servername already exists in the sql.ini or interfaces file.");
		private String       _sqlIniFileName        = "";

		Vector<String>       _profileSuggestions = new Vector<String>();
		private JPanel       _profileName_pan    = null;
		private JLabel       _profileName_head   = new JLabel();
		private JLabel       _profileName_lbl    = new JLabel("As Profile Name");
		private JComboBox    _profileName_cbx    = new JComboBox(_profileSuggestions);
		private JRadioButton _profileNameQ1_rbt  = new JRadioButton("Add Profile", true);
		private JRadioButton _profileNameQ2_rbt  = new JRadioButton("Not this time");
		private JRadioButton _profileNameQ3_rbt  = new JRadioButton("Never ask this question again");
		private JLabel       _profileName_bussy  = new JLabel("The selected Profile Name already exists, choose another one.");

		private JButton      _ok_but     = new JButton("OK");
		private JButton      _cancel_but = new JButton("Cancel");

		public SaveAsDialog(JDialog owner, String dbServerName, ConnectionProfile connProfile, boolean afterSuccessfulConnect)
		{
			super(owner, "Add Server", true);
			
			_owner        = owner;
//			_inKey        = inKey;
			_dbServerName = dbServerName;
			_connProfile  = connProfile;
			_afterSuccessfulConnect = afterSuccessfulConnect;
			
			init();
			pack();
			setLocationRelativeTo(owner);

			validateCompenents();

			// Focus to 'OK', escape to 'CANCEL'
			SwingUtils.installEscapeButton(this, _cancel_but);
			SwingUtils.setFocus(_ok_but);
		}
		
		private void init()
		{
			boolean showInterfacesPanel  = false;
			boolean showConnProfilePanel = true;

			if (_connProfile.isType(ConnectionProfile.Type.TDS))
			{
				ConnectionProfile.TdsEntry tdsEntry = (ConnectionProfile.TdsEntry)_connProfile.getEntry();
				_sqlIniFileName = tdsEntry._tdsIfile;

				String hostPortStr     = _connProfile.getKey();
				String ifileServerName = AseConnectionFactory.getIServerName(hostPortStr);
				
				// Check if the server exists (do we need to show)
				boolean showPanel = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_ifile_serverAdd_showDialog, DEFAULT_ifile_serverAdd_showDialog);
				if (StringUtil.isNullOrBlank(ifileServerName) && showPanel)
				{
					showInterfacesPanel = true;
				}

				// Compose a possible server name
				String suggestedName = "";
				if (StringUtil.isNullOrBlank(suggestedName))
				{
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
				}

				// Add some suggestions to the interfaces
				if (StringUtil.hasValue(_dbServerName)) _interfacesSuggestions.add(_dbServerName);
				if (StringUtil.hasValue(suggestedName)) _interfacesSuggestions.add(suggestedName);
				_interfacesSuggestions.add(hostPortStr.replace(':', '_').replace('.', '_'));
				
				// Add some suggestions to the profile
				if (StringUtil.hasValue(_dbServerName))            _profileSuggestions.add(_dbServerName);
				if (StringUtil.hasValue(suggestedName))            _profileSuggestions.add(suggestedName);
				if (StringUtil.hasValue(ifileServerName))          _profileSuggestions.add(ifileServerName);
				_profileSuggestions.add(hostPortStr.replace(':', '_').replace('.', '_').replace(',', '_'));

				if ( StringUtil.hasValue(_connProfile.getName()) && ! _profileSuggestions.contains(_connProfile.getName()) )
				{
					if (_connProfile.getName().endsWith("_copy"))
						_profileSuggestions.add(0, _connProfile.getName());
					else
						_profileSuggestions.add(_connProfile.getName());
				}
			}
			else
			{
    			if (StringUtil.hasValue(_dbServerName))            _profileSuggestions.add(_dbServerName);
    			_profileSuggestions.add(_connProfile.getKey());

    			// If profile name hasn't already been added, DO IT
    			// If it's a COPY, put the copy entry FIRST
				if ( StringUtil.hasValue(_connProfile.getName()) && ! _profileSuggestions.contains(_connProfile.getName()) )
				{
					if (_connProfile.getName().endsWith("_copy"))
						_profileSuggestions.add(0, _connProfile.getName());
					else
						_profileSuggestions.add(_connProfile.getName());
				}
			}

			_interfacesName_bussy.setForeground(Color.RED);
			_interfacesName_cbx.getEditor().getEditorComponent().addKeyListener(new KeyAdapter()
			{
				@Override
				public void keyReleased(KeyEvent e)
				{
					validateCompenents();
				}
			});

			_profileName_bussy.setForeground(Color.RED);
			_profileName_cbx.getEditor().getEditorComponent().addKeyListener(new KeyAdapter()
			{
				@Override
				public void keyReleased(KeyEvent e)
				{
					validateCompenents();
				}
			});

			String interfacesMsg =
					"<html>" +
					"<h3>Add server to sql.ini or interfaces file</h3>" +
					"The server name was <b>not</b> found in file <code>" + _sqlIniFileName + "</code><br>" +
					"Do you want to add it to the interfaces file?<br>" +
					"<br>" +
					"Please <b>choose a name from the drop down list below</b>, or type your own name<br>" +
					"</html>";

			String profileMsg = 
					"<html>" +
					"<h3>Add "+_connProfile.getSrvType()+" server to Connection Profile</h3>" +
					"The server name was <b>not</b> found in file <code>" + getFilename() + "</code><br>" +
					"Do you want to add it to Connection Profile?<br>" +
					"<br>" +
					"Please <b>choose a name from the drop down list below</b>, or type your own name<br>" +
					"</html>";

			_interfacesName_head.setText(interfacesMsg);
			_profileName_head   .setText(profileMsg);

    		_interfacesName_cbx.setEditable(true);
			_profileName_cbx   .setEditable(true);

			if (_interfacesName_cbx.getItemCount() > 0)	_interfacesName_cbx.setSelectedIndex(0);
			if (_profileName_cbx   .getItemCount() > 0) _profileName_cbx   .setSelectedIndex(0);
    
			_interfacesName_pan = SwingUtils.createPanel("Name Service", true);
			_profileName_pan    = SwingUtils.createPanel("Connection Profile", true);

			setLayout(new MigLayout());
			_interfacesName_pan.setLayout(new MigLayout());
			_profileName_pan   .setLayout(new MigLayout());


			ButtonGroup interfacesButGroup = new ButtonGroup();
			interfacesButGroup.add(_interfacesQ1_rbt);
			interfacesButGroup.add(_interfacesQ2_rbt);
			interfacesButGroup.add(_interfacesQ3_rbt);
			
			ButtonGroup profileButGroup = new ButtonGroup();
			profileButGroup.add(_profileNameQ1_rbt);
			profileButGroup.add(_profileNameQ2_rbt);
			profileButGroup.add(_profileNameQ3_rbt);
			
			_interfacesName_pan.add(_interfacesName_head,  "span, wrap 20");
			_interfacesName_pan.add(_interfacesName_lbl,   "");
			_interfacesName_pan.add(_interfacesName_cbx,   "pushx, growx, wrap");
			_interfacesName_pan.add(_interfacesName_bussy, "skip, pushx, growx, hidemode 3, wrap");
			_interfacesName_pan.add(_interfacesQ1_rbt,     "skip, split");
			_interfacesName_pan.add(_interfacesQ2_rbt,     "");
			_interfacesName_pan.add(_interfacesQ3_rbt,     "wrap");
    
			_profileName_pan.add(_profileName_head,        "span, wrap 20");
			_profileName_pan.add(_profileName_lbl,         "");
			_profileName_pan.add(_profileName_cbx,         "pushx, growx, wrap");
			_profileName_pan.add(_profileName_bussy,       "skip, pushx, growx, hidemode 3, wrap");
			_profileName_pan.add(_profileNameQ1_rbt,       "skip, split");
			_profileName_pan.add(_profileNameQ2_rbt,       "");
			_profileName_pan.add(_profileNameQ3_rbt,       "wrap");

			add(_interfacesName_pan, "push, grow, hidemode 3, wrap");
			add(_profileName_pan,    "push, grow, hidemode 3, wrap");
			add(_ok_but,             "split, tag ok");
			add(_cancel_but,         "tag cancel");
			
			_interfacesName_pan.setVisible(showInterfacesPanel);
			_profileName_pan   .setVisible(showConnProfilePanel);

			_ok_but    .addActionListener(this);
			_cancel_but.addActionListener(this);
		}

		private void validateCompenents()
		{
			boolean enableOk = true;

			if (_interfacesName_pan.isVisible())
			{
				String currentItem = _interfacesName_cbx.getEditor().getItem()+"";
				boolean bussy = AseConnectionFactory.getIHostPortStr(currentItem) != null;
				_interfacesName_bussy.setVisible(bussy);

				if (bussy)
					enableOk = false;
			}

			if (_profileName_pan.isVisible())
			{
				String currentItem = _profileName_cbx.getEditor().getItem()+"";
				boolean bussy = getProfile(currentItem) != null;
				_profileName_bussy.setVisible(bussy);

				if (bussy)
					enableOk = false;
			}
				
			_ok_but.setEnabled(enableOk);
		}
		
		@Override
		public void actionPerformed(ActionEvent e)
		{
			Object source = e.getSource();
			
			if (_ok_but.equals(source))
			{
				//-----------------------------------
				// PROFILE
				if (_profileName_pan.isVisible())
				{
					//-----------------------------------
					// Add Profile
					if (_profileNameQ1_rbt.isSelected())
					{
						String entry = StringUtil.getSelectedItemString(_profileName_cbx);
						_connProfile.setName(entry);

						addProfile(_connProfile, true);
					}
					//-----------------------------------
					// Not This Time
					if (_profileNameQ2_rbt.isSelected())
					{
					}
					//-----------------------------------
					// Never ask this question again
					if (_profileNameQ3_rbt.isSelected())
					{
						Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
						if (conf != null)
						{
							conf.setProperty(PROPKEY_connProfile_serverAdd_showDialog, !DEFAULT_ifile_serverAdd_showDialog);
							conf.save();
						}
					}
				}

				//-----------------------------------
				// INTERFACES
				if (_interfacesName_pan.isVisible())
				{
					//-----------------------------------
					// Add Server
					if (_interfacesQ1_rbt.isSelected())
					{
						String entry = StringUtil.getSelectedItemString(_interfacesName_cbx);
						
						boolean canWrite = FileUtils.canWrite(_sqlIniFileName);
						if (canWrite)
							AseConnectionFactory.addIFileEntry(_sqlIniFileName, entry, _connProfile.getKey());
						else
						{
							// Copy the file to $HOME/.asetune/sql.ini and add it there???
							String newFile = copyInterfacesFileToPrivateFile(_sqlIniFileName, _owner);
							if (newFile != null)
								AseConnectionFactory.addIFileEntry(newFile, entry, _connProfile.getKey());
						}
					}
					//-----------------------------------
					// Not This Time
					if (_interfacesQ2_rbt.isSelected())
					{
					}
					//-----------------------------------
					// Never ask this question again
					if (_interfacesQ3_rbt.isSelected())
					{
						Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
						if (conf != null)
						{
							conf.setProperty(PROPKEY_ifile_serverAdd_showDialog, !DEFAULT_ifile_serverAdd_showDialog);
							conf.save();
						}
					}
				}
				
				setVisible(false);
			} // end OK
			
			if (_cancel_but.equals(source))
			{
				setVisible(false);
			}
		}
		
		public void showPossibly()
		{
			if (_interfacesName_pan.isVisible() || _profileName_pan.isVisible())
				setVisible(true);
		}
	}

	private boolean saveChangesDialog(String key, ConnectionProfile connProfile, ConnProfileEntry newConnProfileEntry, String dbServerName, boolean afterSuccessfulConnect, JDialog owner)
	{
		boolean showDialog = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_connProfile_changed_showDialog, DEFAULT_connProfile_changed_showDialog);
		boolean alwaysSave = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_connProfile_changed_alwaysSave, DEFAULT_connProfile_changed_alwaysSave);

		if (afterSuccessfulConnect)
		{
			if ( ! showDialog )
			{
				if (alwaysSave)
					setProfileEntry(connProfile, newConnProfileEntry);
				return alwaysSave;
			}
		}

		String changesTable = connProfile.getDifference(newConnProfileEntry);

		String htmlMsg = "<html>" +
				"<h3>Connection Attributes has been changed</h3>" +
				"Profile Name '<b>"+connProfile.getName()+"</b>'.<br>" +
				"You have changed some Attributes since the last time you connected with this profile.<br>" +
				"<br>" +
				"Below is the changes made:<br>" +
				changesTable +
				"<br>" +
				"Do you want to change the stored profile to reflect your changes?<br>" +
				"</html>";

		final String SAVE_THIS     = "Save Changes";
		final String SAVE_AS       = "Save As New Profile";
		final String NOT_THIS_TIME = "Not this time";
		final String ALWAYS        = "Always Save Changes";
		final String NEVER         = "Never Save Changes";
		String[] options;
		if (afterSuccessfulConnect)
			options = new String[] { SAVE_THIS, SAVE_AS, NOT_THIS_TIME, ALWAYS, NEVER };
		else
			options = new String[] { SAVE_THIS, SAVE_AS, NOT_THIS_TIME };

		int result = JOptionPane.showOptionDialog(owner, htmlMsg, "Save Changes", 
				JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, 0);
		String choice = options[result];
		
		// Handle results from the popup question
		if (SAVE_THIS.equals(choice))
		{
			setProfileEntry(connProfile, newConnProfileEntry);
			return true;
		}

		if (SAVE_AS.equals(choice))
		{
			ConnectionProfile connProfileCopy = connProfile.copy(connProfile, connProfile.getName()+"_copy");
			connProfileCopy.setEntry(newConnProfileEntry);
//			setProfileEntry(connProfileCopy, newConnProfileEntry);

    		SaveAsDialog saveAs = new SaveAsDialog(owner, dbServerName, connProfileCopy, afterSuccessfulConnect);
    		saveAs.showPossibly();
    		return false;
		}

		if (NOT_THIS_TIME.equals(choice))
		{
			return false;
		}

		if (ALWAYS.equals(choice))
		{
			Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
			if (conf != null)
			{
				conf.setProperty(PROPKEY_connProfile_changed_showDialog, false);
				conf.setProperty(PROPKEY_connProfile_changed_alwaysSave, true);
				conf.save();
			}
			return true;
		}

		if (NEVER.equals(choice))
		{
			Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
			if (conf != null)
			{
				conf.setProperty(PROPKEY_connProfile_changed_showDialog, false);
				conf.setProperty(PROPKEY_connProfile_changed_alwaysSave, false);
				conf.save();
			}
			return false;
		}
		
		return true;
	}

//	/**
//	 * Ask user a question what name the profile should have
//	 * 
//	 * @param inKey
//	 * @param dbServerName
//	 * @param connProfile
//	 * @return
//	 */
//	private boolean saveAsName(String inKey, String dbServerName, ConnectionProfile connProfile)
//	{
//		String propName = PROPKEY_tds_serverAdd_doNotAskAgain;
//		if (connProfile.isType(Type.TDS))     propName = PROPKEY_tds_serverAdd_doNotAskAgain;
//		if (connProfile.isType(Type.JDBC))    propName = PROPKEY_jdbc_serverAdd_doNotAskAgain;
//		if (connProfile.isType(Type.OFFLINE)) propName = PROPKEY_offline_serverAdd_doNotAskAgain;
//
//		boolean defAsk = true;
//		if (connProfile.isType(Type.TDS))     defAsk = DEFAULT_tds_serverAdd_doNotAskAgain;
//		if (connProfile.isType(Type.JDBC))    defAsk = DEFAULT_jdbc_serverAdd_doNotAskAgain;
//		if (connProfile.isType(Type.OFFLINE)) defAsk = DEFAULT_offline_serverAdd_doNotAskAgain;
//
//		boolean doNotAskAgain = Configuration.getCombinedConfiguration().getBooleanProperty(propName, defAsk);
//System.out.println("saveAsName(inKey='"+inKey+"', connProfile='"+connProfile+"'): doNotAskAgain="+doNotAskAgain+", propName='"+propName+"', defAsk="+defAsk);
//		if (doNotAskAgain==false)
//		{
//			String htmlMsg = "<html>" +
//					"<h3>Add "+connProfile.getSrvType()+" server to Connection Profile</h3>" +
//					"The server name was <b>not</b> found in file <code>" + getFilename() + "</code><br>" +
//					"Do you want to add it to Connection Profile?<br>" +
//					"<br>" +
//					"Please <b>choose a name from the drop down list below</b>, or type your own name<br>" +
//					"</html>";
//
//			// Compose a possible server name
//			String suggestedName = "";
//			// FIXME: add some more logic to the above
//
//System.out.println("saveAsName(): dbServerName='"+dbServerName+"'.");
//// Add more options in the below dialog
//// - KEY
//// - servername
//// - something else...
//			
//			if (connProfile.isType(Type.TDS))
//			{
//				// Get servername from interfaces file
//				if (StringUtil.isNullOrBlank(suggestedName))
//					suggestedName = AseConnectionFactory.getIServerName(inKey);
//
//				// Not found in the interfaces file, try to compose a name HOSTNAME_PORTNUMBER
//				if (StringUtil.isNullOrBlank(suggestedName))
//				{
//					Map<String, String> hostPortMap = StringUtil.parseCommaStrToMap(inKey, ":", ",");
//					for (String key : hostPortMap.keySet())
//					{
//						String val = hostPortMap.get(key);
//						
//						// If first entry is localhost, get next entry
//						if (key.equalsIgnoreCase("localhost") && hostPortMap.size() > 1)
//							continue;
//						
//						suggestedName = key.toUpperCase();
//						if (suggestedName.indexOf(".") > 0) // hostname.acme.com
//							suggestedName = suggestedName.substring(0, suggestedName.indexOf("."));
//						suggestedName += "_" + val;
//						
//						break; // only get first entry
//					}
//				}
//			}
//			
//			// Add some suggestions to the list
//			Vector<String> suggestions = new Vector<String>();
//			if (StringUtil.hasValue(dbServerName))  suggestions.add(dbServerName);
//			if (StringUtil.hasValue(suggestedName)) suggestions.add(suggestedName);
//			suggestions.add(inKey);
//			
//			JLabel     header    = new JLabel(htmlMsg);
//			JLabel     label     = new JLabel("As Name");
////			JTextField entryName = new JTextField(suggestedName, 30);
//			JComboBox entryName = new JComboBox(suggestions);
//			JPanel     panel     = new JPanel(new MigLayout());
//			panel.add(header,    "span, wrap 20");
//			panel.add(label,     "");
//			panel.add(entryName, "pushx, growx, wrap 20");
//
//			entryName.setEditable(true);
//
//			String[] options = { "Add Server", "Not this time", "Never ask this question again" };
//			int result = JOptionPane.showOptionDialog(null, panel, "Add Server Name", 
//					JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, 0);
//
//			// Handle results from the popup question
//			if (result == 0) // Add Server
//			{
//				String entry = entryName.getSelectedItem()+"";
//
//				connProfile.setName(entry);
//				return true;
//			}
//			if (result == 1) // Not this time
//			{
//				return false;
//			}
//			if (result == 2) // Never ask this question again
//			{
//				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
//				if (conf != null)
//				{
//					conf.setProperty(propName, true);
//					conf.save();
//				}
//				return false;
//			}
//		}
//		return false;
//	}
//
//	private boolean saveChanges(String key, ConnectionProfile connProfile)
//	{
//		// FIXME: implemet this
//		return true;
//	}
//
//	
//	
//	
//	
//	private void checkAddEntryToInterfacesFile(String sqlIniFileName, String hostPortStr, String dbServerName)
//	{
//		// Server name was NOT found in the interfaces file
//		// Ask if you want to add it to the interfaces file
//		String server = AseConnectionFactory.getIServerName(hostPortStr);
//		
//		boolean doNotAskAgain = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_ifile_serverAdd_doNotAskAgain, DEFAULT_ifile_serverAdd_doNotAskAgain);
//		if (StringUtil.isNullOrBlank(server) && doNotAskAgain==false)
//		{
//			// Check if the sql.ini / interfaces is writable... before we edit the file...
//			// if it's read only - maybe ask if we should copy the file and add the entry...
//			String htmlMsg = "<html>" +
//					"<h3>Add server to sql.ini or interfaces file</h3>" +
//					"The server name was <b>not</b> found in file <code>" + sqlIniFileName + "</code><br>" +
//					"Do you want to add it to the interfaces file?<br>" +
//					"<br>" +
//					"Please <b>choose a name from the drop down list below</b>, or type your own name<br>" +
//					"</html>";
//
//			// Compose a possible server name
//			String suggestedName = "";
//			if (StringUtil.isNullOrBlank(suggestedName))
//			{
//    			Map<String, String> hostPortMap = StringUtil.parseCommaStrToMap(hostPortStr, ":", ",");
//    			for (String key : hostPortMap.keySet())
//    			{
//    				String val = hostPortMap.get(key);
//    
//    				// If first entry is localhost, get next entry
//    				if (key.equalsIgnoreCase("localhost") && hostPortMap.size() > 1)
//    					continue;
//    
//    				suggestedName = key.toUpperCase();
//    				if (suggestedName.indexOf(".") > 0) // hostname.acme.com
//    					suggestedName = suggestedName.substring(0, suggestedName.indexOf("."));
//    				suggestedName += "_" + val;
//    
//    				break; // only get first entry
//    			}
//			}
//
//			// Add some suggestions to the list
//			Vector<String> suggestions = new Vector<String>();
//			if (StringUtil.hasValue(dbServerName))  suggestions.add(dbServerName);
//			if (StringUtil.hasValue(suggestedName)) suggestions.add(suggestedName);
//			suggestions.add(hostPortStr.replace(':', '_').replace('.', '_'));
//			
//			JLabel     header    = new JLabel(htmlMsg);
//			JLabel     label     = new JLabel("As Server Name");
////			JTextField entryName = new JTextField(suggestedName, 30);
//			JComboBox entryName = new JComboBox(suggestions);
//			JPanel     panel     = new JPanel(new MigLayout());
//			panel.add(header,    "span, wrap 20");
//			panel.add(label,     "");
//			panel.add(entryName, "pushx, growx, wrap 20");
//
//			entryName.setEditable(true);
//
//			String[] options = { "Add Server", "Not this time", "Never ask this question again" };
//			int result = JOptionPane.showOptionDialog(null, panel, "Add Server Name", 
//					JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, 0);
//
//			if (result == 0) // save
//			{
//				String entry = entryName.getSelectedItem()+"";
//				
//				boolean canWrite = FileUtils.canWrite(sqlIniFileName);
//				if (canWrite)
//					AseConnectionFactory.addIFileEntry(sqlIniFileName, entry, hostPortStr);
//				else
//				{
////					String htmlStr = "<html>" +
////							"<h3>Warning</h3>" +
////							"Name service file '"+sqlIniFileName+"' is <b>not writable</b>" +
////							"So adding/changing entries will be impossible!<br>" +
////							"</html>";
////					SwingUtils.showWarnMessage(null, "not writable", htmlStr, null);
//
//					// Copy the file to $HOME/.asetune/sql.ini and add it there???
//					String newFile = copyInterfacesFileToPrivateFile(sqlIniFileName);
//					if (newFile != null)
//						AseConnectionFactory.addIFileEntry(newFile, entry, hostPortStr);
//				}
//			}
//			if (result == 2) // never
//			{
//				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
//				if (conf != null)
//				{
//					conf.setProperty(PROPKEY_ifile_serverAdd_doNotAskAgain, true);
//					conf.save();
//				}
//			}
//		}
//	}


	public String copyInterfacesFileToPrivateFile(String currentInterfacesFile, JDialog owner)
	{
		String newFileName = null;

		String privateSqlIni = AseConnectionFactory.getPrivateInterfacesFile(false);
		
		// If load file and private is not the same file AND "load" is read only
		// Should we copy the "source" file to the "private" file 
		if ( ! privateSqlIni.equalsIgnoreCase(currentInterfacesFile) )
		{
			boolean show = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_ifile_copyReadOnly_showDialog, DEFAULT_ifile_copyReadOnly_showDialog);
			if (show)
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
				int result = JOptionPane.showOptionDialog(owner, htmlMsg, "Copy File?", 
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
						SwingUtils.showWarnMessage(owner, "Copy File: Problems", htmlMsg, ex);
					}
				}
				if (result == 2) // Never ask this question again
				{
					Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
					if (conf != null)
					{
						conf.setProperty(PROPKEY_ifile_copyReadOnly_showDialog, !DEFAULT_ifile_copyReadOnly_showDialog);
						conf.save();
					}
				}
	    	} // end: showDialog == false
		} // end:  privateSqlIni != file
    	
    	return newFileName;
	}










	private static class FilteredTreeModel
	extends DefaultTreeModel
	{
		private static final long serialVersionUID = 1L;
		public FilteredTreeModel(TreeNode root)
		{
			super(root);
		}

		private String _filterProductName = null;
		
		public void   setFilterOnProductName(String name) { _filterProductName = name; }
		public String getFilterOnProductName()            { return _filterProductName; }

		private boolean filterInclude(DefaultMutableTreeNode node)
		{
			Object o = node.getUserObject();
			if (o instanceof ConnectionProfile) 
			{
				ConnectionProfile connProfile = (ConnectionProfile) o;
	
				// Hide Server types of NOT desired products
				if (_filterProductName != null)
				{
					if (DbUtils.isProductName(_filterProductName, DbUtils.DB_PROD_NAME_SYBASE_ASE))
					{
						if ( ! connProfile.isSrvType(ConnectionProfile.SrvType.TDS_ASE) )
						{
							//System.out.println("HIDE NODE: "+connProfile);
							return false;
						}
					}
				}
			}
			return true;
		}
		
		@Override
		public Object getChild(Object parent, int index)
		{
			DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) parent;
			int filteredIndex = -1;
			for (int i=0; i<parentNode.getChildCount(); i++)
			{
				DefaultMutableTreeNode child = (DefaultMutableTreeNode) parentNode.getChildAt(i);

				if ( filterInclude(child) )
					filteredIndex++;

				if ( filteredIndex == index )
					return child;
			}
			return null;
		}

		@Override
		public int getChildCount(Object parent)
		{
			DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) parent;
			int childCount = 0;
			for (int i=0; i<parentNode.getChildCount(); i++)
			{
				DefaultMutableTreeNode child = (DefaultMutableTreeNode) parentNode.getChildAt(i);

				if ( filterInclude(child) )
					childCount++;
			}
			return childCount;
		}

		@Override
		public boolean isLeaf(Object node)
		{
			return getChildCount(node) < 1;
		}
		
	}

	/**
	 * Creates a default layout of the Connection Profile XML storage file
	 * 
	 * @param filename
	 */
	private void createStorageFile(String filename)
	{
		File f = new File(filename);
		if (f.exists())
		{
			_logger.error("ConnectionProfileManager.createStorageFile(): The file '"+filename+"' already exists, I will not overvrite it.");
			return;
		}
		
		_logger.info("Creating a new file '"+filename+"' for Connection Profile Manager to store profile information.");
		save(filename, true);
	}


	public void save()
	{
		save(getFilename());
	}

	public void save(String filename)
	{
		save(getFilename(), false);
	}

	private void save(String filename, boolean writeTemplateFile)
	{
		if (StringUtil.isNullOrBlank(filename))
			filename = getFilename();

		LinkedHashMap<String, Throwable> problemMap = new LinkedHashMap<String, Throwable>();
		String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss-SSS").format(new Date());
		String tmpRecoveryFilename = filename + "." + timestamp + ".recover";

		try
		{
			File f = new File(filename);
			if (f.exists())
			{
				_logger.debug("ConnectionProfileManager.save(): saving a recovery file to '"+tmpRecoveryFilename+"', which will be deleted if no problems where found.");
				FileUtils.copy(filename, tmpRecoveryFilename);
			}

			RandomAccessFile raf = new RandomAccessFile(filename, "rw");
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
					sb.append("<").append(ConnectionProfile.XML_CONN_PROF_ENTRIES).append(">\n");
					sb.append("\n");

					//-----------------------------------------
					// Write header
					ByteBuffer byteBuffer;
					byteBuffer = ByteBuffer.wrap(sb.toString().getBytes(Charset.forName("UTF-8")));
					channel.write(byteBuffer);


					//--------------------------------------
					// Write Catalog Structure
					if (writeTemplateFile)
					{
						sb.setLength(0);
						
						sb.append("    ").append("<").append(XML_PROFILE_TREE_CATALOG).append(" ").append(XML_PROFILE_TREE_CATALOG_ATTR_NAME).append("=\"").append("Production").append("\">\n");
						sb.append("    ").append("</").append(XML_PROFILE_TREE_CATALOG).append(">\n");
						sb.append("\n");
						sb.append("    ").append("<").append(XML_PROFILE_TREE_CATALOG).append(" ").append(XML_PROFILE_TREE_CATALOG_ATTR_NAME).append("=\"").append("Development").append("\">\n");
						sb.append("    ").append("</").append(XML_PROFILE_TREE_CATALOG).append(">\n");
						sb.append("\n");
						sb.append("    ").append("<").append(XML_PROFILE_TREE_CATALOG).append(" ").append(XML_PROFILE_TREE_CATALOG_ATTR_NAME).append("=\"").append("Test").append("\">\n");
						sb.append("    ").append("</").append(XML_PROFILE_TREE_CATALOG).append(">\n");
						
						byteBuffer = ByteBuffer.wrap(sb.toString().getBytes(Charset.forName("UTF-8")));
						channel.write(byteBuffer);
					}
					else
					{
						if (_profileTreeRoot != null && _profileTreeRoot.getChildCount() > 0)
						{
							sb.setLength(0);
							
							sb.append("    <").append(XML_PROFILE_TREE).append(">\n");
							sb.append(getXmlProfileTree(_profileTreeRoot, 2));
							sb.append("    </").append(XML_PROFILE_TREE).append(">\n");
							sb.append("\n");
							
							byteBuffer = ByteBuffer.wrap(sb.toString().getBytes(Charset.forName("UTF-8")));
							channel.write(byteBuffer);
						}
						else
						{
							Exception error = new Exception("_profileTreeRoot.getChildCount()="+(_profileTreeRoot == null ? "null" : _profileTreeRoot.getChildCount())+", _profileTreeRoot="+_profileTreeRoot);
							_logger.warn("ConnectionProfileManager trying to save to file '"+filename+"', but ROOT entry seems to 'empty'. For debug reasons, save a stacktrace", error);
						}
					}

					//--------------------------------------
					// Write TDS/JDBC/OFFLINE entries
					if (writeTemplateFile)
					{
						// If we want to write a DUMMY Connection entry, this is the place to do it.
					}
					else
					{
						for (ConnectionProfile entry : getProfiles().values())
						{
							sb.setLength(0);
							
							try
							{
								sb.append(entry.toXml());
								sb.append("\n");
							}
							catch (Throwable t)
							{
								problemMap.put(entry.getName(), t);
								_logger.error("Problems writing XML ENTRY for name='"+entry.getName()+"', type="+entry.getType()+", srvType="+entry.getSrvType()+" to file '"+filename+"'. Continuing with next entry. Caught: "+t, t);
							}
							
							byteBuffer = ByteBuffer.wrap(sb.toString().getBytes(Charset.forName("UTF-8")));
							channel.write(byteBuffer);
						}
					}

					
					//-----------------------------------------
					// Write -end- entries
					sb.setLength(0);
					
					sb.append("\n");
					sb.append("</").append(ConnectionProfile.XML_CONN_PROF_ENTRIES).append(">\n");


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
				raf.close();
			}			
		}
		catch (IOException e)
		{
			_logger.warn("Problems writing to Connection Profile file '"+filename+"'. Caught: "+e);
			
			String htmlMsg = 
					"<html>"
					+ "<h3>Problems writing to Connection Profile storage file</h3>"
					+ "Storage file name: <code>"+filename+"</code><br>"
					+ "<br>"
					+ "A copy was made of the file before the save process started.<br>"
					+ "Recovery file: <code>"+tmpRecoveryFilename+"</code><br>"
					+ "<br>"
					+ "Exception: " + e +"<br>"
					+ "</html>";
			SwingUtils.showErrorMessage("Write error", htmlMsg, e);
//			throw e;
		}

		// Remove the recovery file if everything succeeded
		if (problemMap.size() == 0)
		{
			_logger.debug("ConnectionProfileManager.save(): SAVE-SUCCEED: removing the recovery file '"+tmpRecoveryFilename+"'.");
			File f = new File(tmpRecoveryFilename);
			f.delete();
		}
		else
		{
			_logger.error("Some problems saving the Connection Profile Storage file '"+filename+"', problemCount="+problemMap.size()+". A backup/restore file has been saved as '"+tmpRecoveryFilename+"' if you want to revert back to that file.");

			String problemEntries = "";
			Throwable firstEx = null;
			for (String name : problemMap.keySet())
			{
				problemEntries += "'<b>" + name + "</b>', ";
				if (firstEx == null)
					firstEx = problemMap.get(name);
			}

			String htmlMsg = 
					"<html>"
					+ "<h3>Problems writing to Connection Profile storage file</h3>"
					+ "Found some issues when writing some Connection Profile Entries<br>"
					+ "problemCount: " + problemMap.size() +"<br>"
					+ "problemEntries: " + problemEntries +"<br>"
					+ "<br>"
					+ "Storage file name: <code>"+filename+"</code><br>"
					+ "<br>"
					+ "A copy was made of the file before the save process started.<br>"
					+ "Recovery file: <code>"+tmpRecoveryFilename+"</code><br>"
					+ "<br>"
					+ "If the storage file is <b>corrupt</b> you may want to revert back to the recovery file.<br>"
					+ "</html>";
			SwingUtils.showErrorMessage("Write error", htmlMsg, firstEx);
		}
	}

	private String getXmlProfileTree(DefaultMutableTreeNode node, int nestLevel)
	{
		StringBuilder sb = new StringBuilder();
		String indentStr = StringUtil.replicate(" ", nestLevel*4);
		
		@SuppressWarnings("rawtypes")
		Enumeration e = node.children();
		while (e.hasMoreElements())
		{
			DefaultMutableTreeNode thisNode = (DefaultMutableTreeNode) e.nextElement();
			Object o = thisNode.getUserObject();

			if (o instanceof ConnectionProfile)
			{
				String name = ((ConnectionProfile)o).getName();
				sb.append(indentStr).append("<").append(XML_PROFILE_TREE_ENTRY).append(" ").append(XML_PROFILE_TREE_ENTRY_ATTR_NAME).append("=\"").append(name).append("\"/>\n");
			}
			else if (o instanceof ConnectionProfileCatalog)
			{
				String name = ((ConnectionProfileCatalog)o).getName();

				sb.append(indentStr).append("<").append(XML_PROFILE_TREE_CATALOG).append(" ").append(XML_PROFILE_TREE_CATALOG_ATTR_NAME).append("=\"").append(name).append("\">\n");
				sb.append(getXmlProfileTree(thisNode, nestLevel+1));
				sb.append(indentStr).append("</").append(XML_PROFILE_TREE_CATALOG).append(">\n");
			}
			else
			{
				_logger.warn("getXmlProfileTree(): object is unknown type. ClassName='"+o.getClass().getName()+"', toString='"+o+"', nestLevel="+nestLevel+", thisNode='"+thisNode+"'.");
				String name = o.toString();
				sb.append(indentStr).append("<").append(XML_PROFILE_TREE_ENTRY).append(" ").append(XML_PROFILE_TREE_ENTRY_ATTR_NAME).append("=\"").append(name).append("\"/>\n");
			}
		}
		return sb.toString();
	}

	/**
	 * This function converts String XML to Document object
	 * 
	 * @param in - XML String
	 * @return Document object
	 */
	private void parseXmlFile(String filename)
	{
		try
		{
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
//			InputSource is = new InputSource(new StringReader(in));
			Document doc = db.parse(new File(filename));
			
			doc.getDocumentElement().normalize();
			 
//			System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
		 

			NodeList nlist;
			String   subTag;

			//----------------- TdsEntry
			subTag = ConnectionProfile.XML_TDS_ENTRY;
			nlist  = doc.getElementsByTagName(subTag);
			for (int i=0; i<nlist.getLength(); i++) 
			{
				Node node = nlist.item(i);
		 
				_logger.debug("Current Element at '"+subTag+"' :" + node.getNodeName());
				if (node.getNodeType() == Node.ELEMENT_NODE) 
				{
					ConnectionProfile entry = TdsEntry.parseXml((Element) node);
					addProfile(entry, false);
		 		}
			}

			//----------------- JdbcEntry
			subTag = ConnectionProfile.XML_JDBC_ENTRY;
			nlist  = doc.getElementsByTagName(subTag);
			for (int i=0; i<nlist.getLength(); i++) 
			{
				Node node = nlist.item(i);
		 
				_logger.debug("Current Element at '"+subTag+"' :" + node.getNodeName());
				if (node.getNodeType() == Node.ELEMENT_NODE) 
				{
					ConnectionProfile entry = JdbcEntry.parseXml((Element) node);
					addProfile(entry, false);
		 		}
			}

			//----------------- OfflineEntry
			subTag = ConnectionProfile.XML_OFFLINE_ENTRY;
			nlist  = doc.getElementsByTagName(subTag);
			for (int i=0; i<nlist.getLength(); i++) 
			{
				Node node = nlist.item(i);
		 
				_logger.debug("Current Element at '"+subTag+"' :" + node.getNodeName());
				if (node.getNodeType() == Node.ELEMENT_NODE) 
				{
					ConnectionProfile entry = OfflineEntry.parseXml((Element) node);
					addProfile(entry, false);
		 		}
			}

			//----------------- ProfileTree
			subTag = XML_PROFILE_TREE;
			if (doc.getElementsByTagName(subTag).getLength() > 0)
			{
    			nlist  = doc.getElementsByTagName(subTag).item(0).getChildNodes();
    			if (nlist.getLength() > 0)
    			{
    				_profileTreeRoot.removeAllChildren();
    				addToProfileTree(_profileTreeRoot, nlist);
    			}
			}
			if (_profileTreeRoot.getChildCount() == 0)
			{
				_profileTreeRoot.add(new DefaultMutableTreeNode(new ConnectionProfileCatalog("Production"),  true));
				_profileTreeRoot.add(new DefaultMutableTreeNode(new ConnectionProfileCatalog("Development"), true));
				_profileTreeRoot.add(new DefaultMutableTreeNode(new ConnectionProfileCatalog("Test"),        true));
			}
			
			
			// CHECK: if we have Connection Profiles that is not part of the tree, add them
			for (ConnectionProfile cp : getProfiles().values())
			{
				if ( getTreePath(cp) == null )
					_profileTreeRoot.add(new DefaultMutableTreeNode(cp));
			}
		}
		catch (ParserConfigurationException e)
		{
			_logger.error("parseXmlFile(filename='"+filename+"'): Caught: "+e, e);
		}
		catch (SAXException e)
		{
			_logger.error("parseXmlFile(filename='"+filename+"'): Caught: "+e, e);
		}
		catch (IOException e)
		{
			_logger.error("parseXmlFile(filename='"+filename+"'): Caught: "+e, e);
		}
	}


	private void addToProfileTree(DefaultMutableTreeNode treeNode, NodeList nlist)
	{
		for (int i=0; i<nlist.getLength(); i++) 
		{
			Node node = nlist.item(i);
	 
			if (node.getNodeType() == Node.ELEMENT_NODE) 
			{
				String tagName = node.getNodeName();

				if (XML_PROFILE_TREE_CATALOG.equals(tagName))
				{
					String name = ((Element)node).getAttribute(XML_PROFILE_TREE_CATALOG_ATTR_NAME);
					
					DefaultMutableTreeNode newTreeNode = new DefaultMutableTreeNode(new ConnectionProfileCatalog(name), true);
					treeNode.add(newTreeNode);

					addToProfileTree(newTreeNode, node.getChildNodes());
				}
				else if (XML_PROFILE_TREE_ENTRY.equals(tagName))
				{
					String name = ((Element)node).getAttribute(XML_PROFILE_TREE_ENTRY_ATTR_NAME);

					ConnectionProfile connProfile = getProfile(name);
					if (connProfile != null)
					{
						DefaultMutableTreeNode newTreeNode = new DefaultMutableTreeNode(connProfile, false);
						treeNode.add(newTreeNode);
					}
					else
					{
						DefaultMutableTreeNode newTreeNode = new DefaultMutableTreeNode(name, false);
						treeNode.add(newTreeNode);
						_logger.warn("Connection Profile '"+name+"' wasn't found in the list of known connection profiles. Skipping this and continuing with next one...");
					}
				}
				else
				{
					_logger.warn("Connection Profile found unknwon XML tag '"+tagName+"' when parsing file '"+getFilename()+"'. Skipping this and continuing with next one...");
				}
	 		}
		}
	}
}
