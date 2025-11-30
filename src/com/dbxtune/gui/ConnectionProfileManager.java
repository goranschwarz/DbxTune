/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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
package com.dbxtune.gui;

import java.awt.Color;
import java.awt.Container;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.dbxtune.AppDir;
import com.dbxtune.Version;
import com.dbxtune.gui.ConnectionProfile.ConnProfileEntry;
import com.dbxtune.gui.ConnectionProfile.JdbcEntry;
import com.dbxtune.gui.ConnectionProfile.OfflineEntry;
import com.dbxtune.gui.ConnectionProfile.SrvType;
import com.dbxtune.gui.ConnectionProfile.TdsEntry;
import com.dbxtune.utils.AseConnectionFactory;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.FileUtils;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.SwingUtils;
import com.dbxtune.utils.TimeUtils;

import net.miginfocom.swing.MigLayout;

public class ConnectionProfileManager
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
//	private static final long serialVersionUID = 1L;

	/** all connection profiles, by key: getName() from the profile */
	private LinkedHashMap<String, ConnectionProfile> _profileMap = new LinkedHashMap<String, ConnectionProfile>();

	/** all ProfileTypes */
	private LinkedHashMap<String, ProfileType> _profileTypesMap = new LinkedHashMap<String, ProfileType>();

	/** all connection profiles, by key: getKey() from  the profile */
//	private LinkedHashMap<String, ConnectionProfile> _keyMap     = new LinkedHashMap<String, ConnectionProfile>();

	/** root node of the Connection Profile Tree */ 
	private DefaultMutableTreeNode _profileTreeRoot  = new DefaultMutableTreeNode("All", true);
	/** data model for the Connection Profile Tree */ 
	private FilteredTreeModel      _profileTreeModel = new FilteredTreeModel(_profileTreeRoot);


	/** Singleton */
	private static ConnectionProfileManager _instance = null;

	/** Filename holding the configuration */
	private String _filename         = null;
	private long   _fileLastModified = 0;

	/** default file name where the connection profiles are stored */
	public  final static String  PROPKEY_STORAGE_FILE = "ConnectionProfileManager.storage.filename";
	public  final static String  DEFAULT_STORAGE_FILE = AppDir.getDbxUserHomeDir() + File.separator + "ConnectionProfiles.xml";

	public static final String   PROPKEY_connProfile_serverAdd_showDialog_H2_offline = "ConnectionProfileManager.connProfile.serverAdd.showDialog.h2.offline";
	public static final boolean  DEFAULT_connProfile_serverAdd_showDialog_H2_offline = false;

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

	public static final String   PROPKEY_connProfile_show_rootNode        = "ConnectionProfileManager.connProfile.show.rootNode";
	public static final boolean  DEFAULT_connProfile_show_rootNode        = false;

	
	//---------------------------------------------
	// 16 pixel icons
	//---------------------------------------------
	// Sybase products
	public static final ImageIcon ICON_DB_PROD_NAME_16_SYBASE_ASE       = SwingUtils.readImageIcon(Version.class, "images/conn_profile_ase_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_16_SYBASE_ASA       = SwingUtils.readImageIcon(Version.class, "images/conn_profile_asa_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_16_SYBASE_IQ        = SwingUtils.readImageIcon(Version.class, "images/conn_profile_iq_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_16_SYBASE_RS        = SwingUtils.readImageIcon(Version.class, "images/conn_profile_rs_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_16_SYBASE_RAX       = SwingUtils.readImageIcon(Version.class, "images/conn_profile_rax_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_16_SYBASE_RSDRA     = SwingUtils.readImageIcon(Version.class, "images/conn_profile_rsdra_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_16_SYBASE_OTHER     = SwingUtils.readImageIcon(Version.class, "images/conn_profile_tds_other_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_16_SYBASE_UNDEFINED = SwingUtils.readImageIcon(Version.class, "images/conn_profile_tds_undefined_16.png");

	// SAP products
	public static final ImageIcon ICON_DB_PROD_NAME_16_HANA             = SwingUtils.readImageIcon(Version.class, "images/conn_profile_hana_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_16_MAXDB            = SwingUtils.readImageIcon(Version.class, "images/conn_profile_maxdb_16.png");

	// JDBC products
	public static final ImageIcon ICON_DB_PROD_NAME_16_H2               = SwingUtils.readImageIcon(Version.class, "images/conn_profile_h2_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_16_HSQL             = SwingUtils.readImageIcon(Version.class, "images/conn_profile_hsql_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_16_MSSQL            = SwingUtils.readImageIcon(Version.class, "images/conn_profile_mssql_16.png");
//	public static final ImageIcon ICON_DB_PROD_NAME_16_MSSQL_BLACK      = SwingUtils.readImageIcon(Version.class, "images/conn_profile_mssql_black_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_16_ORACLE           = SwingUtils.readImageIcon(Version.class, "images/conn_profile_oracle_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_16_DB2_LUW          = SwingUtils.readImageIcon(Version.class, "images/conn_profile_db2_ux_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_16_DB2_ZOS          = SwingUtils.readImageIcon(Version.class, "images/conn_profile_db2_zos_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_16_MYSQL            = SwingUtils.readImageIcon(Version.class, "images/conn_profile_mysql_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_16_DERBY            = SwingUtils.readImageIcon(Version.class, "images/conn_profile_derby_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_16_POSTGRES         = SwingUtils.readImageIcon(Version.class, "images/conn_profile_postgres_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_16_APACHE_HIVE      = SwingUtils.readImageIcon(Version.class, "images/conn_profile_apache_hive_16.png");

	public static final ImageIcon ICON_DB_PROD_NAME_16_OTHER            = SwingUtils.readImageIcon(Version.class, "images/conn_profile_unknown_vendor_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_16_UNDEFINED        = SwingUtils.readImageIcon(Version.class, "images/conn_profile_jdbc_undefined_16.png");

	//---------------------------------------------
	// 32 pixel icons
	//---------------------------------------------
	// Sybase products
	public static final ImageIcon ICON_DB_PROD_NAME_32_SYBASE_ASE       = SwingUtils.readImageIcon(Version.class, "images/conn_profile_ase_32.png");
	public static final ImageIcon ICON_DB_PROD_NAME_32_SYBASE_ASA       = SwingUtils.readImageIcon(Version.class, "images/conn_profile_asa_32.png");
	public static final ImageIcon ICON_DB_PROD_NAME_32_SYBASE_IQ        = SwingUtils.readImageIcon(Version.class, "images/conn_profile_iq_32.png");
	public static final ImageIcon ICON_DB_PROD_NAME_32_SYBASE_RS        = SwingUtils.readImageIcon(Version.class, "images/conn_profile_rs_32.png");
	public static final ImageIcon ICON_DB_PROD_NAME_32_SYBASE_RAX       = SwingUtils.readImageIcon(Version.class, "images/conn_profile_rax_32.png");
	public static final ImageIcon ICON_DB_PROD_NAME_32_SYBASE_RSDRA     = SwingUtils.readImageIcon(Version.class, "images/conn_profile_rsdra_32.png");
	public static final ImageIcon ICON_DB_PROD_NAME_32_SYBASE_OTHER     = SwingUtils.readImageIcon(Version.class, "images/conn_profile_tds_other_32.png");
	public static final ImageIcon ICON_DB_PROD_NAME_32_SYBASE_UNDEFINED = SwingUtils.readImageIcon(Version.class, "images/conn_profile_tds_undefined_32.png");

	// SAP products
	public static final ImageIcon ICON_DB_PROD_NAME_32_HANA             = SwingUtils.readImageIcon(Version.class, "images/conn_profile_hana_32.png");
	public static final ImageIcon ICON_DB_PROD_NAME_32_MAXDB            = SwingUtils.readImageIcon(Version.class, "images/conn_profile_maxdb_32.png");

	// JDBC products
	public static final ImageIcon ICON_DB_PROD_NAME_32_H2               = SwingUtils.readImageIcon(Version.class, "images/conn_profile_h2_32.png");
	public static final ImageIcon ICON_DB_PROD_NAME_32_HSQL             = SwingUtils.readImageIcon(Version.class, "images/conn_profile_hsql_32.png");
	public static final ImageIcon ICON_DB_PROD_NAME_32_MSSQL            = SwingUtils.readImageIcon(Version.class, "images/conn_profile_mssql_32.png");
//	public static final ImageIcon ICON_DB_PROD_NAME_32_MSSQL_BLACK      = SwingUtils.readImageIcon(Version.class, "images/conn_profile_mssql_black_32.png");
	public static final ImageIcon ICON_DB_PROD_NAME_32_ORACLE           = SwingUtils.readImageIcon(Version.class, "images/conn_profile_oracle_32.png");
	public static final ImageIcon ICON_DB_PROD_NAME_32_DB2_LUW          = SwingUtils.readImageIcon(Version.class, "images/conn_profile_db2_ux_32.png");
	public static final ImageIcon ICON_DB_PROD_NAME_32_DB2_ZOS          = SwingUtils.readImageIcon(Version.class, "images/conn_profile_db2_zos_32.png");
	public static final ImageIcon ICON_DB_PROD_NAME_32_MYSQL            = SwingUtils.readImageIcon(Version.class, "images/conn_profile_mysql_32.png");
	public static final ImageIcon ICON_DB_PROD_NAME_32_DERBY            = SwingUtils.readImageIcon(Version.class, "images/conn_profile_derby_32.png");
	public static final ImageIcon ICON_DB_PROD_NAME_32_POSTGRES         = SwingUtils.readImageIcon(Version.class, "images/conn_profile_postgres_32.png");
	public static final ImageIcon ICON_DB_PROD_NAME_32_APACHE_HIVE      = SwingUtils.readImageIcon(Version.class, "images/conn_profile_apache_hive_32.png");

	public static final ImageIcon ICON_DB_PROD_NAME_32_OTHER            = SwingUtils.readImageIcon(Version.class, "images/conn_profile_unknown_vendor_32.png");
	public static final ImageIcon ICON_DB_PROD_NAME_32_UNDEFINED        = SwingUtils.readImageIcon(Version.class, "images/conn_profile_jdbc_undefined_32.png");

	//-------------------------------------------------------------------------------------------------------------------
	// XML Strings for the Catalog and ConnProfile Tree
	private final static String XML_PROFILE_TREE                       = "ProfileTree";
	private final static String XML_PROFILE_TREE_CATALOG               = "Catalog";
	private final static String XML_PROFILE_TREE_CATALOG_ATTR_NAME     = "name";
	private final static String XML_PROFILE_TREE_CATALOG_ATTR_EXPANDED = "expanded";
	private final static String XML_PROFILE_TREE_ENTRY                 = "Entry";
	private final static String XML_PROFILE_TREE_ENTRY_ATTR_NAME       = "name";

	//-------------------------------------------------------------------------------------------------------------------
	//  XML Strings for the ProfileTypes
	public static final String       XML_PROFILE_TYPES                            = "ProfileTypes";
	public static final String       XML_PROFILE_TYPE_ENTRY                       = "ProfileTypeEntry";
	public static final String       XML_PROFILE_TYPE_ENTRY_ATTR_name             = "name";
	public static final String       XML_PROFILE_TYPE_ENTRY_COLOR                 = "Color";
	public static final String       XML_PROFILE_TYPE_ENTRY_COLOR_ATTR_r          = "r";
	public static final String       XML_PROFILE_TYPE_ENTRY_COLOR_ATTR_g          = "g";
	public static final String       XML_PROFILE_TYPE_ENTRY_COLOR_ATTR_b          = "b";
	public static final String       XML_PROFILE_TYPE_ENTRY_BORDER                = "Border";
	public static final String       XML_PROFILE_TYPE_ENTRY_BORDER_ATTR_top       = "top";
	public static final String       XML_PROFILE_TYPE_ENTRY_BORDER_ATTR_left      = "left";
	public static final String       XML_PROFILE_TYPE_ENTRY_BORDER_ATTR_bottom    = "bottom";
	public static final String       XML_PROFILE_TYPE_ENTRY_BORDER_ATTR_right     = "right";
	
	public static class ProfileType
	{
		public String        _name          = "unknown";
		public Color         _color         = Color.WHITE;
		public Insets        _borderMargins = new Insets(0, 0, 0, 0);;

		public ProfileType()
		{
		}

		public ProfileType(String name, Color color, Insets borderMargins)
		{
			_name          = name;
			_color         = color;
			_borderMargins = borderMargins;
		}
		
		public ProfileType(String name)
		{
			_name          = name;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (obj == null) return false;
			if (!(obj instanceof ProfileType)) return false;	
			if (obj == this) return true;

			ProfileType paramType = (ProfileType) obj;
			return    equals(this._name,          paramType._name) 
			       && equals(this._color,         paramType._color) 
			       && equals(this._borderMargins, paramType._borderMargins);
		}
		private boolean equals(Object control, Object test)
		{
			if (null == control)
				return null == test;
			return control.equals(test);
		}
		
		public String getName()
		{
			return _name;
		}

		public static String getColorRgbStr(Color color)
		{
			if (color == null)
				return "rgb(255,255,255)"; // WHITE

			return "rgb("+color.getRed()+","+color.getGreen()+","+color.getBlue()+")";
		}
		public String getColorRgbStr()
		{
			return getColorRgbStr(_color);
		}
		public String getMarginStr()
		{
			return "top="+_borderMargins.top+", left="+_borderMargins.left+", bottom="+_borderMargins.bottom+", right="+_borderMargins.right;
		}

		@Override
		/** Used for displaying the value in JComboBox etc... */
		public String toString()
		{
			// Get the default background color of the ComboBox
			Color defaultComboBoxBgColor = UIManager.getColor("ComboBox.background");
//			Color defaultComboBoxBgColor = UIManager.getColor("TextField.background");
			if (defaultComboBoxBgColor == null)
				defaultComboBoxBgColor = Color.WHITE;

			// If background is the same as the ProfileType, then do not add any color... it will just look strange when selection the value
			// otherwise: lets use HTML coloring...
			if (defaultComboBoxBgColor.equals(_color))
				return "<html>"+getName()+"</html>"; // Still do HTML rendering of the string 
			else
				return "<html><font fgcolor='black', bgcolor='"+getColorRgbStr()+"'>"+getName()+"</font></html>"; // Note this is used by the JComboBox to "color" the items
//			return "<html>"+getName()+"<i><font bgcolor='"+getColorRgbStr()+"'> - Color</font></i></html>"; // Note this is used by the JComboBox to "color" the items
//			return "<html><font fgcolor='black', bgcolor='"+getColorRgbStr()+"'>"+getName()+"</font></html>"; // Note this is used by the JComboBox to "color" the items
//			return super.toString() + "; name='"+_name+"', color{"+getColorRgbStr()+"}, borderMargins{top="+_borderMargins.top+",left="+_borderMargins.left+",bottom="+_borderMargins.bottom+",right="+_borderMargins.right+"}";
//			return super.toString() + "; name='"+_name+"', color{r="+_color.getRed()+",g="+_color.getGreen()+",b="+_color.getBlue()+"}, borderMargins{top="+_borderMargins.top+",left="+_borderMargins.left+",bottom="+_borderMargins.bottom+",right="+_borderMargins.right+"}";
		}

		public String toXml()
		{
			StringBuilder sb = new StringBuilder();
			sb.append("        <").append(XML_PROFILE_TYPE_ENTRY).append(" ").append(XML_PROFILE_TYPE_ENTRY_ATTR_name).append("=\"").append(_name).append("\"").append(">\n");
			sb.append("            <").append(XML_PROFILE_TYPE_ENTRY_COLOR).append(" ").append(XML_PROFILE_TYPE_ENTRY_COLOR_ATTR_r).append("=\"").append( _color.getRed()   ).append("\"")
			                                                               .append(" ").append(XML_PROFILE_TYPE_ENTRY_COLOR_ATTR_g).append("=\"").append( _color.getGreen() ).append("\"")
			                                                               .append(" ").append(XML_PROFILE_TYPE_ENTRY_COLOR_ATTR_b).append("=\"").append( _color.getBlue()  ).append("\"")
			                                                               .append("/>\n");
			sb.append("            <").append(XML_PROFILE_TYPE_ENTRY_BORDER).append(" ").append(XML_PROFILE_TYPE_ENTRY_BORDER_ATTR_top)   .append("=\"").append( _borderMargins.top    ).append("\"")
			                                                                .append(" ").append(XML_PROFILE_TYPE_ENTRY_BORDER_ATTR_left)  .append("=\"").append( _borderMargins.left   ).append("\"")
			                                                                .append(" ").append(XML_PROFILE_TYPE_ENTRY_BORDER_ATTR_bottom).append("=\"").append( _borderMargins.bottom ).append("\"")
			                                                                .append(" ").append(XML_PROFILE_TYPE_ENTRY_BORDER_ATTR_right) .append("=\"").append( _borderMargins.right  ).append("\"")
			                                                                .append("/>\n");
			sb.append("        </").append(XML_PROFILE_TYPE_ENTRY).append(">\n");
			return sb.toString();
		}

		public static ProfileType parseXml(Element element)
		{
			ProfileType entry = new ProfileType();
			
			entry._name = element.getAttribute(XML_PROFILE_TYPE_ENTRY_ATTR_name);
			
			NodeList colorList = element.getElementsByTagName(XML_PROFILE_TYPE_ENTRY_COLOR);
			if (colorList.getLength() > 0)
			{
				Node node = colorList.item(0);
				if (node.getNodeType() == Node.ELEMENT_NODE) 
				{
					Element el = (Element) node;
					int r = StringUtil.parseInt(el.getAttribute(XML_PROFILE_TYPE_ENTRY_COLOR_ATTR_r), 0);
					int g = StringUtil.parseInt(el.getAttribute(XML_PROFILE_TYPE_ENTRY_COLOR_ATTR_g), 0);
					int b = StringUtil.parseInt(el.getAttribute(XML_PROFILE_TYPE_ENTRY_COLOR_ATTR_b), 0);
					entry._color = new Color(r, g, b);
				}
			}
			
			NodeList borderList = element.getElementsByTagName(XML_PROFILE_TYPE_ENTRY_BORDER);
			if (borderList.getLength() > 0)
			{
				Node node = borderList.item(0);
				if (node.getNodeType() == Node.ELEMENT_NODE) 
				{
					Element el = (Element) node;
					int top    = StringUtil.parseInt(el.getAttribute(XML_PROFILE_TYPE_ENTRY_BORDER_ATTR_top   ), 0);
					int left   = StringUtil.parseInt(el.getAttribute(XML_PROFILE_TYPE_ENTRY_BORDER_ATTR_left  ), 0);
					int bottom = StringUtil.parseInt(el.getAttribute(XML_PROFILE_TYPE_ENTRY_BORDER_ATTR_bottom), 0);
					int right  = StringUtil.parseInt(el.getAttribute(XML_PROFILE_TYPE_ENTRY_BORDER_ATTR_right ), 0);
					entry._borderMargins = new Insets(top, left, bottom, right);
				}
			}

			return entry;
		}
	}

//<ProfileTypes>
//  <ProfileTypeEntry name="Development">
//		<Color r="0" g="255" b="0"/>
//		<Border top="2" left="3" bottom="2" right="3"/>
//  </ProfileTypeEntry>
//  <ProfileTypeEntry name="Test">
//		<Color r="0" g="255" b="0"/>
//		<Border top="2" left="3" bottom="2" right="3"/>
//  </ProfileTypeEntry>
//  <ProfileTypeEntry name="Integration">
//		<Color r="0" g="255" b="0"/>
//		<Border top="2" left="3" bottom="2" right="3"/>
//  </ProfileTypeEntry>
//  <ProfileTypeEntry name="Staging">
//		<Color r="0" g="255" b="0"/>
//		<Border top="2" left="3" bottom="2" right="3"/>
//  </ProfileTypeEntry>
//  <ProfileTypeEntry name="Production">
//		<Color r="255" g="0" b="0"/>
//		<Border top="2" left="3" bottom="2" right="3"/>
//  </ProfileTypeEntry>
//</ProfileTypes>

	
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
			_filename         = filename;
			_fileLastModified = f.lastModified();
			parseXmlFile(filename);
		}
	}

	/**
	 * Reload from current file or storage
	 */
	public void reload()
	{
		String filename = getFilename();
		_logger.info("Re-loading Connection Profile Manage using filename='"+filename+"'.");
		setFilename(filename);
		_profileTreeModel.reload(); // this will call: fireTreeStructureChanged(...)
	}

	/**
	 * Check if the storage has changed
	 */
	public boolean hasStorageChanged()
	{
		File f = new File(getFilename());
		if (f.lastModified() > _fileLastModified)
		{
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String prevDateStr = sdf.format(new Date(_fileLastModified));
			String newDateStr  = sdf.format(new Date(f.lastModified()));

			_logger.info("The underlying Connection Profile Manage storage was has been changed. prevDate='"+prevDateStr+"', newDate='"+newDateStr+"', filename='"+f+"'.");
			return true;
		}
		return false;
	}

	/**
	 * Get all saved ProfileType objects
	 * @return a LinkedHasMap of <name,ProfileType> (will never be null, if none found it will be an empty list)
	 */
	public LinkedHashMap<String, ProfileType> getProfileTypes()
	{
		return _profileTypesMap;
	}

	/**
	 * A ProfileType object, associated to the name
	 * <p>
	 * @param name name of the profileType we want to get
	 * @return A ProfileType object, if not found null will be returned
	 */
	public ProfileType getProfileTypeByName(String name)
	{
		if (StringUtil.isNullOrBlank(name))
			return null;

		ProfileType profileType = _profileTypesMap.get(name);

		return profileType;
	}

	public void addProfileType(ProfileType entry)
	{
		_profileTypesMap.put(entry.getName(), entry);
	}

//	public void setProfileTypes(LinkedHashMap<String, ProfileType> entryMap)
//	{
//		_profileTypesMap = entryMap;
//	}
	public void setProfileTypes(List<ProfileType> entryList)
	{
		addProfileTypes(entryList, true);
	}
	public void addProfileTypes(List<ProfileType> entryList, boolean clearBeforeAdd)
	{
		if (clearBeforeAdd)
			_profileTypesMap.clear();

		for (ProfileType entry : entryList)
			_profileTypesMap.put(entry.getName(), entry);
	}

	private void createDefaultProfileTypes()
	{
		addProfileType( new ProfileType("Development", Color.WHITE,  new Insets(0, 0, 0, 0)) );
		addProfileType( new ProfileType("Test",        Color.WHITE,  new Insets(0, 0, 0, 0)) );
		addProfileType( new ProfileType("Integration", Color.YELLOW, new Insets(2, 3, 2, 3)) );
		addProfileType( new ProfileType("Staging",     Color.ORANGE, new Insets(2, 3, 2, 3)) );
		addProfileType( new ProfileType("Production",  Color.RED,    new Insets(2, 3, 2, 3)) );
	}


	/**
	 * A ConnectionProfile object, associated to the name
	 * 
	 * @param name name of the profile we want to get
	 * @return A connection object, if not found null will be returned
	 */
	public ConnectionProfile getFirstProfileThatStartsWith(String name)
	{
		if (StringUtil.isNullOrBlank(name))
			return null;

		for (ConnectionProfile cp : _profileMap.values())
		{
			if (cp.getName().startsWith(name))
				return cp;
		}
		return null;
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
	 * @param fromProfileNode 
	 */
	public void addProfile(ConnectionProfile connProfile, boolean addToTree, DefaultMutableTreeNode catalogNode, DefaultMutableTreeNode originProfileNode)
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
				try
				{
					DefaultMutableTreeNode insertAtNode = _profileTreeRoot;
					if (catalogNode != null)
						insertAtNode = catalogNode;

					// Determine WHERE to insert the node in the tree "path"
					// if    originProfileNode was passed: add it AFTER that entry
					// else: put it at the end of the current "path"
					int insIndex = insertAtNode.getChildCount();
					if (originProfileNode != null)
					{
						int tmpInsIndex = insertAtNode.getIndex(originProfileNode);
						if (tmpInsIndex != -1)
						{
							insIndex = tmpInsIndex + 1;
						}
					}
					
					_profileTreeModel.insertNodeInto(
							new DefaultMutableTreeNode(connProfile), 
							insertAtNode, 
							insIndex);
				}
				catch (Throwable t) 
				{
					_logger.warn("Problems Add a ConnectionProfile named '"+connProfile.getName()+"'. Trying to continuing anyway... Caught: "+t, t);
				}
			}
		}
//		save();
	}
	
	public void addProfile(ConnectionProfile connProfile, boolean addToTree, String catalogName)
	{
		// get current selected TreeNode (so we can add the new profile in the correct directory)
		DefaultMutableTreeNode catalogNode = getCatalogTreeNodeForEntry(catalogName);
		
		addProfile(connProfile, addToTree, catalogNode, null);
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
//System.out.println("ConnectionProfileManager.setProfileEntry(): connProfile='"+connProfile.getName()+"', connProfileEntry='"+connProfileEntry.toXml("xxx", Type.TDS, SrvType.TDS_ASE)+"'.");
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
			try
			{
				_profileTreeModel.nodeChanged(node[node.length-1]);
			}
			catch(Throwable t)
			{
				_logger.debug("Problems when updating the tree node '"+node[node.length-1]+"'. Caught: "+t, t);
			}
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
//		Enumeration<DefaultMutableTreeNode> en = _profileTreeRoot.preorderEnumeration();
		Enumeration<TreeNode> en = _profileTreeRoot.preorderEnumeration();
		while (en.hasMoreElements())
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) en.nextElement();

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
		if (hasStorageChanged())
			reload();

		_profileTreeModel.setFilterOnProductName(filterOnProductName);
		return _profileTreeModel;
	}

	public void setTreeModelFilterOnProductName(String name) 
	{ 
		_profileTreeModel.setFilterOnProductName(name);
		_profileTreeModel.reload();
	}
	public String getTreeModelFilterOnProductName()            
	{ 
		return _profileTreeModel.getFilterOnProductName(); 
	}


	public void setTreeModelFilterOnProfileName(String name) 
	{ 
		_profileTreeModel.setFilterOnProfileName(name);
		_profileTreeModel.reload();
	}
	public String getTreeModelFilterOnProfileName()            
	{ 
		return _profileTreeModel.getFilterOnProfileName(); 
	}


//	public static ImageIcon getIcon16_forIconMerge(String productName)
//	{
//		if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_MSSQL       )) 
//			return ICON_DB_PROD_NAME_16_MSSQL_BLACK;
//
//		return getIcon16(productName);
//	}

	public static ImageIcon getIcon16(String productName)
	{
		if      (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_DB2_LUW     )) return ICON_DB_PROD_NAME_16_DB2_LUW;
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_DB2_ZOS     )) return ICON_DB_PROD_NAME_16_DB2_ZOS;
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_DERBY       )) return ICON_DB_PROD_NAME_16_DERBY;
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_H2          )) return ICON_DB_PROD_NAME_16_H2;
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_HANA        )) return ICON_DB_PROD_NAME_16_HANA;
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_MAXDB       )) return ICON_DB_PROD_NAME_16_MAXDB;
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_HSQL        )) return ICON_DB_PROD_NAME_16_HSQL;
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_MSSQL       )) return ICON_DB_PROD_NAME_16_MSSQL;
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_MYSQL       )) return ICON_DB_PROD_NAME_16_MYSQL;
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_ORACLE      )) return ICON_DB_PROD_NAME_16_ORACLE;
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_POSTGRES    )) return ICON_DB_PROD_NAME_16_POSTGRES;
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_APACHE_HIVE )) return ICON_DB_PROD_NAME_16_APACHE_HIVE;
		
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_SYBASE_ASA  )) return ICON_DB_PROD_NAME_16_SYBASE_ASA;
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_SYBASE_ASE  )) return ICON_DB_PROD_NAME_16_SYBASE_ASE;
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_SYBASE_IQ   )) return ICON_DB_PROD_NAME_16_SYBASE_IQ;
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_SYBASE_RS   )) return ICON_DB_PROD_NAME_16_SYBASE_RS;
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_SYBASE_RAX  )) return ICON_DB_PROD_NAME_16_SYBASE_RAX;
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_SYBASE_RSDRA)) return ICON_DB_PROD_NAME_16_SYBASE_RSDRA;

		return ICON_DB_PROD_NAME_16_OTHER;
	}
	public static ImageIcon getIcon32(String productName)
	{
		if      (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_DB2_LUW     )) return ICON_DB_PROD_NAME_32_DB2_LUW;
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_DB2_ZOS     )) return ICON_DB_PROD_NAME_32_DB2_ZOS;
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_DERBY       )) return ICON_DB_PROD_NAME_32_DERBY;
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_H2          )) return ICON_DB_PROD_NAME_32_H2;
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_HANA        )) return ICON_DB_PROD_NAME_32_HANA;
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_MAXDB       )) return ICON_DB_PROD_NAME_32_MAXDB;
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_HSQL        )) return ICON_DB_PROD_NAME_32_HSQL;
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_MSSQL       )) return ICON_DB_PROD_NAME_32_MSSQL;
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_MYSQL       )) return ICON_DB_PROD_NAME_32_MYSQL;
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_ORACLE      )) return ICON_DB_PROD_NAME_32_ORACLE;
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_POSTGRES    )) return ICON_DB_PROD_NAME_32_POSTGRES;
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_APACHE_HIVE )) return ICON_DB_PROD_NAME_32_APACHE_HIVE;
		                                                                             
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_SYBASE_ASA  )) return ICON_DB_PROD_NAME_32_SYBASE_ASA;
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_SYBASE_ASE  )) return ICON_DB_PROD_NAME_32_SYBASE_ASE;
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_SYBASE_IQ   )) return ICON_DB_PROD_NAME_32_SYBASE_IQ;
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_SYBASE_RS   )) return ICON_DB_PROD_NAME_32_SYBASE_RS;
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_SYBASE_RAX  )) return ICON_DB_PROD_NAME_32_SYBASE_RAX;
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_SYBASE_RSDRA)) return ICON_DB_PROD_NAME_32_SYBASE_RSDRA;

		return ICON_DB_PROD_NAME_32_OTHER;
	}
	public static ImageIcon getIcon16(SrvType srvType)
	{
		if      (SrvType.JDBC_DB2_LUW    .equals(srvType)) return ICON_DB_PROD_NAME_16_DB2_LUW;
		else if (SrvType.JDBC_DB2_ZOS    .equals(srvType)) return ICON_DB_PROD_NAME_16_DB2_ZOS;
		else if (SrvType.JDBC_DERBY      .equals(srvType)) return ICON_DB_PROD_NAME_16_DERBY;
		else if (SrvType.JDBC_H2         .equals(srvType)) return ICON_DB_PROD_NAME_16_H2;
		else if (SrvType.JDBC_HANA       .equals(srvType)) return ICON_DB_PROD_NAME_16_HANA;
		else if (SrvType.JDBC_MAXDB      .equals(srvType)) return ICON_DB_PROD_NAME_16_MAXDB;
		else if (SrvType.JDBC_HSQL       .equals(srvType)) return ICON_DB_PROD_NAME_16_HSQL;
		else if (SrvType.JDBC_MSSQL      .equals(srvType)) return ICON_DB_PROD_NAME_16_MSSQL;
		else if (SrvType.JDBC_MYSQL      .equals(srvType)) return ICON_DB_PROD_NAME_16_MYSQL;
		else if (SrvType.JDBC_ORACLE     .equals(srvType)) return ICON_DB_PROD_NAME_16_ORACLE;
		else if (SrvType.JDBC_POSTGRES   .equals(srvType)) return ICON_DB_PROD_NAME_16_POSTGRES;
		else if (SrvType.JDBC_APACHE_HIVE.equals(srvType)) return ICON_DB_PROD_NAME_16_APACHE_HIVE;
		else if (SrvType.JDBC_OTHER      .equals(srvType)) return ICON_DB_PROD_NAME_16_OTHER;
		else if (SrvType.JDBC_UNDEFINED  .equals(srvType)) return ICON_DB_PROD_NAME_16_UNDEFINED;
                                         
		else if (SrvType.TDS_ASA         .equals(srvType)) return ICON_DB_PROD_NAME_16_SYBASE_ASA;
		else if (SrvType.TDS_ASE         .equals(srvType)) return ICON_DB_PROD_NAME_16_SYBASE_ASE;
		else if (SrvType.TDS_IQ          .equals(srvType)) return ICON_DB_PROD_NAME_16_SYBASE_IQ;
		else if (SrvType.TDS_RS          .equals(srvType)) return ICON_DB_PROD_NAME_16_SYBASE_RS;
		else if (SrvType.TDS_RAX         .equals(srvType)) return ICON_DB_PROD_NAME_16_SYBASE_RAX;
		else if (SrvType.TDS_RSDRA       .equals(srvType)) return ICON_DB_PROD_NAME_16_SYBASE_RSDRA;
		else if (SrvType.TDS_OTHER       .equals(srvType)) return ICON_DB_PROD_NAME_16_SYBASE_OTHER;
		else if (SrvType.TDS_UNDEFINED   .equals(srvType)) return ICON_DB_PROD_NAME_16_SYBASE_UNDEFINED;

		return ICON_DB_PROD_NAME_16_OTHER;
	}

	public static ImageIcon getIcon32(SrvType srvType)
	{
		if      (SrvType.JDBC_DB2_LUW    .equals(srvType)) return ICON_DB_PROD_NAME_32_DB2_LUW;
		else if (SrvType.JDBC_DB2_ZOS    .equals(srvType)) return ICON_DB_PROD_NAME_32_DB2_ZOS;
		else if (SrvType.JDBC_DERBY      .equals(srvType)) return ICON_DB_PROD_NAME_32_DERBY;
		else if (SrvType.JDBC_H2         .equals(srvType)) return ICON_DB_PROD_NAME_32_H2;
		else if (SrvType.JDBC_HANA       .equals(srvType)) return ICON_DB_PROD_NAME_32_HANA;
		else if (SrvType.JDBC_MAXDB      .equals(srvType)) return ICON_DB_PROD_NAME_32_MAXDB;
		else if (SrvType.JDBC_HSQL       .equals(srvType)) return ICON_DB_PROD_NAME_32_HSQL;
		else if (SrvType.JDBC_MSSQL      .equals(srvType)) return ICON_DB_PROD_NAME_32_MSSQL;
		else if (SrvType.JDBC_MYSQL      .equals(srvType)) return ICON_DB_PROD_NAME_32_MYSQL;
		else if (SrvType.JDBC_ORACLE     .equals(srvType)) return ICON_DB_PROD_NAME_32_ORACLE;
		else if (SrvType.JDBC_POSTGRES   .equals(srvType)) return ICON_DB_PROD_NAME_32_POSTGRES;
		else if (SrvType.JDBC_APACHE_HIVE.equals(srvType)) return ICON_DB_PROD_NAME_32_APACHE_HIVE;
		else if (SrvType.JDBC_OTHER      .equals(srvType)) return ICON_DB_PROD_NAME_32_OTHER;
		else if (SrvType.JDBC_UNDEFINED  .equals(srvType)) return ICON_DB_PROD_NAME_32_UNDEFINED;
                                         
		else if (SrvType.TDS_ASA         .equals(srvType)) return ICON_DB_PROD_NAME_32_SYBASE_ASA;
		else if (SrvType.TDS_ASE         .equals(srvType)) return ICON_DB_PROD_NAME_32_SYBASE_ASE;
		else if (SrvType.TDS_IQ          .equals(srvType)) return ICON_DB_PROD_NAME_32_SYBASE_IQ;
		else if (SrvType.TDS_RS          .equals(srvType)) return ICON_DB_PROD_NAME_32_SYBASE_RS;
		else if (SrvType.TDS_RAX         .equals(srvType)) return ICON_DB_PROD_NAME_32_SYBASE_RAX;
		else if (SrvType.TDS_RSDRA       .equals(srvType)) return ICON_DB_PROD_NAME_32_SYBASE_RSDRA;
		else if (SrvType.TDS_OTHER       .equals(srvType)) return ICON_DB_PROD_NAME_32_SYBASE_OTHER;
		else if (SrvType.TDS_UNDEFINED   .equals(srvType)) return ICON_DB_PROD_NAME_32_SYBASE_UNDEFINED;

		return ICON_DB_PROD_NAME_32_OTHER;
	}

	public static ImageIcon getIcon16byUrl(String jdbcUrl)
	{
		if (StringUtil.isNullOrBlank(jdbcUrl))
			return ICON_DB_PROD_NAME_16_OTHER;

		if      (jdbcUrl.startsWith("jdbc:odbc:"))                return ICON_DB_PROD_NAME_16_OTHER;
		else if (jdbcUrl.startsWith("jdbc:sybase:Tds:"))          return ICON_DB_PROD_NAME_16_SYBASE_UNDEFINED;
		else if (jdbcUrl.startsWith("jdbc:jtds:"))                return ICON_DB_PROD_NAME_16_MSSQL;
		else if (jdbcUrl.startsWith("jdbc:h2:"))                  return ICON_DB_PROD_NAME_16_H2;
		else if (jdbcUrl.startsWith("jdbc:sap:"))                 return ICON_DB_PROD_NAME_16_HANA;
		else if (jdbcUrl.startsWith("jdbc:sapdb:"))               return ICON_DB_PROD_NAME_16_MAXDB;
		else if (jdbcUrl.startsWith("jdbc:oracle:thin:"))         return ICON_DB_PROD_NAME_16_ORACLE;
		else if (jdbcUrl.startsWith("jdbc:microsoft:sqlserver:")) return ICON_DB_PROD_NAME_16_MSSQL;
		else if (jdbcUrl.startsWith("jdbc:sqlserver:"))           return ICON_DB_PROD_NAME_16_MSSQL;
		else if (jdbcUrl.startsWith("jdbc:db2:"))                 return ICON_DB_PROD_NAME_16_DB2_LUW;
		else if (jdbcUrl.startsWith("jdbc:postgresql:"))          return ICON_DB_PROD_NAME_16_POSTGRES;
		else if (jdbcUrl.startsWith("jdbc:hive2:"))               return ICON_DB_PROD_NAME_16_APACHE_HIVE;
		else if (jdbcUrl.startsWith("jdbc:mysql:"))               return ICON_DB_PROD_NAME_16_MYSQL;
		else if (jdbcUrl.startsWith("jdbc:derby:"))               return ICON_DB_PROD_NAME_16_DERBY;

		return ICON_DB_PROD_NAME_16_OTHER;
	}

	public static ImageIcon getIcon32byUrl(String jdbcUrl)
	{
		if (StringUtil.isNullOrBlank(jdbcUrl))
			return ICON_DB_PROD_NAME_32_OTHER;

		if      (jdbcUrl.startsWith("jdbc:odbc:"))                return ICON_DB_PROD_NAME_32_OTHER;
		else if (jdbcUrl.startsWith("jdbc:sybase:Tds:"))          return ICON_DB_PROD_NAME_32_SYBASE_UNDEFINED;
		else if (jdbcUrl.startsWith("jdbc:jtds:"))                return ICON_DB_PROD_NAME_32_MSSQL;
		else if (jdbcUrl.startsWith("jdbc:h2:"))                  return ICON_DB_PROD_NAME_32_H2;
		else if (jdbcUrl.startsWith("jdbc:sap:"))                 return ICON_DB_PROD_NAME_32_HANA;
		else if (jdbcUrl.startsWith("jdbc:sapdb:"))               return ICON_DB_PROD_NAME_32_MAXDB;
		else if (jdbcUrl.startsWith("jdbc:oracle:thin:"))         return ICON_DB_PROD_NAME_32_ORACLE;
		else if (jdbcUrl.startsWith("jdbc:microsoft:sqlserver:")) return ICON_DB_PROD_NAME_32_MSSQL;
		else if (jdbcUrl.startsWith("jdbc:sqlserver:"))           return ICON_DB_PROD_NAME_32_MSSQL;
		else if (jdbcUrl.startsWith("jdbc:db2:"))                 return ICON_DB_PROD_NAME_32_DB2_LUW;
		else if (jdbcUrl.startsWith("jdbc:postgresql:"))          return ICON_DB_PROD_NAME_32_POSTGRES;
		else if (jdbcUrl.startsWith("jdbc:hive2:"))               return ICON_DB_PROD_NAME_32_APACHE_HIVE;
		else if (jdbcUrl.startsWith("jdbc:mysql:"))               return ICON_DB_PROD_NAME_32_MYSQL;
		else if (jdbcUrl.startsWith("jdbc:derby:"))               return ICON_DB_PROD_NAME_32_DERBY;

		return ICON_DB_PROD_NAME_32_OTHER;
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
	public void possiblyAddChange(String key, boolean afterSuccessfulConnect, String productName, String dbServerName, ConnProfileEntry connProfileEntry, String selectedProfileName, JDialog owner, boolean showProfileOverride)
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

			SaveAsDialog saveAs = new SaveAsDialog(owner, dbServerName, connProfile, connProfile, afterSuccessfulConnect, showProfileOverride);
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

				if (storedTdsEntry._isDbxTuneParamsValid && !newTdsEntry._isDbxTuneParamsValid)
					newTdsEntry.copyDbxTuneParams(storedTdsEntry);
			}

			if (connProfileEntry instanceof JdbcEntry && connProfile.getEntry() instanceof JdbcEntry)
			{
				JdbcEntry storedJdbcEntry = (JdbcEntry) connProfile.getEntry();
				JdbcEntry newJdbcEntry    = (JdbcEntry) connProfileEntry;

				if (storedJdbcEntry._isDbxTuneParamsValid && !newJdbcEntry._isDbxTuneParamsValid)
					newJdbcEntry.copyDbxTuneParams(storedJdbcEntry);
			}

			if ( ! connProfile.equals(connProfileEntry) )
			{
				saveChangesDialog(key, connProfile, connProfileEntry, dbServerName, afterSuccessfulConnect, owner, showProfileOverride);
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
		private ConnectionProfile _originConnProfile = null;
		@SuppressWarnings("unused")
		private boolean           _afterSuccessfulConnect = false;

		Vector<String>            _interfacesSuggestions = new Vector<String>();
		private JPanel            _interfacesName_pan    = null;
		private JLabel            _interfacesName_head   = new JLabel();
		private JLabel            _interfacesName_lbl    = new JLabel("As Server Name");
		private JComboBox<String> _interfacesName_cbx    = new JComboBox<String>(_interfacesSuggestions);
		private JRadioButton      _interfacesQ1_rbt      = new JRadioButton("Add Server", true);
		private JRadioButton      _interfacesQ2_rbt      = new JRadioButton("Not this time");
		private JRadioButton      _interfacesQ3_rbt      = new JRadioButton("Never ask this question again");
		private JLabel            _interfacesName_bussy  = new JLabel("The selected servername already exists in the sql.ini or interfaces file.");
		private String            _sqlIniFileName        = "";
                                  
		Vector<String>            _profileSuggestions = new Vector<String>();
		private JPanel            _profileName_pan    = null;
		private JLabel            _profileName_head   = new JLabel();
		private JLabel            _profileName_lbl    = new JLabel("As Profile Name");
		private JComboBox<String> _profileName_cbx    = new JComboBox<String>(_profileSuggestions);
		private JRadioButton      _profileNameQ1_rbt  = new JRadioButton("Add Profile", true);
		private JRadioButton      _profileNameQ2_rbt  = new JRadioButton("Not this time");
		private JRadioButton      _profileNameQ3_rbt  = new JRadioButton("Never ask this question again");
		private JLabel            _profileName_bussy  = new JLabel("The selected Profile Name already exists, choose another one.");
                                  
		private JButton           _ok_but     = new JButton("OK");
		private JButton           _cancel_but = new JButton("Cancel");

		public SaveAsDialog(JDialog owner, String dbServerName, ConnectionProfile originConnProfile, ConnectionProfile connProfile, boolean afterSuccessfulConnect, boolean showProfileOverride)
		{
			super(owner, "Add Server", true);
			
			_owner                  = owner;
//			_inKey                  = inKey;
			_dbServerName           = dbServerName;
			_originConnProfile      = originConnProfile;
			_connProfile            = connProfile;
			_afterSuccessfulConnect = afterSuccessfulConnect;
			
			init(showProfileOverride);
			pack();
			setLocationRelativeTo(owner);

			validateCompenents();

			// Focus to 'OK', escape to 'CANCEL'
			SwingUtils.installEscapeButton(this, _cancel_but);
			SwingUtils.setFocus(_ok_but);
		}
		
		private void init(boolean showProfileOverride)
		{
			boolean showInterfacesPanel  = false;
			boolean showConnProfilePanel = true;

			showConnProfilePanel = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_connProfile_serverAdd_showDialog, DEFAULT_connProfile_serverAdd_showDialog);
			if (showProfileOverride)
				showConnProfilePanel = true;

			// If it's a OFFLINE session and its a H2 URL, then: DO NOT SHOW THE save panel
			if (_connProfile.isType(ConnectionProfile.Type.OFFLINE) && _connProfile.getOfflineEntry()._jdbcUrl.startsWith("jdbc:h2:"))
			{
				boolean showDialog_H2_offline = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_connProfile_serverAdd_showDialog_H2_offline, DEFAULT_connProfile_serverAdd_showDialog_H2_offline);
				if ( ! showDialog_H2_offline )
				{
					_logger.info("Do not show the 'Save dialog' for H2 OFFLINE Sessions... Normally we dont want to store that in the profile. To enable this for H2, set property: "+PROPKEY_connProfile_serverAdd_showDialog_H2_offline+"=true");
					showConnProfilePanel = false;
				}
			}
				
			
			String userName = _connProfile.getDbUserName();
			if (StringUtil.hasValue(userName))
				userName = " - " + userName;

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
	    
	    				// Get first part (hostname) of the hostname.somedomain.com
	    				suggestedName = key.toUpperCase();
	    				if (suggestedName.indexOf(".") > 0) // hostname.acme.com
	    					suggestedName = suggestedName.substring(0, suggestedName.indexOf("."));
	    				// If it's a number, then it must be an IP address, do not add this, set suggestedName to "" (a blank string)
	    				try { Integer.parseInt(suggestedName); suggestedName = ""; } 
	    				catch(NumberFormatException ignore)  { suggestedName += "_" + val; } // If not a Number, add '_portNumber' at the end
	    				
	    				break; // only get first entry
	    			}
				}

				// Add some suggestions to the interfaces
				if (StringUtil.hasValue(_dbServerName)) _interfacesSuggestions.add(_dbServerName);
				if (StringUtil.hasValue(suggestedName)) _interfacesSuggestions.add(suggestedName);
				_interfacesSuggestions.add(hostPortStr.replace(':', '_').replace('.', '_'));
				
				// Add some suggestions to the profile
				if (StringUtil.hasValue(_dbServerName))            _profileSuggestions.add(_dbServerName   + userName);
				if (StringUtil.hasValue(suggestedName))            _profileSuggestions.add(suggestedName   + userName);
				if (StringUtil.hasValue(ifileServerName))          _profileSuggestions.add(ifileServerName + userName);
				_profileSuggestions.add(hostPortStr.replace(':', '_').replace('.', '_').replace(',', '_')  + userName);

				if ( StringUtil.hasValue(_connProfile.getName()) && ! _profileSuggestions.contains(_connProfile.getName()) )
				{
					if (_connProfile.getName().endsWith("_copy"))
						_profileSuggestions.add(0, _connProfile.getName());
					else
						_profileSuggestions.add(_connProfile.getName() + userName);
				}
			}
			else
			{
    			if (StringUtil.hasValue(_dbServerName))
    				_profileSuggestions.add(_dbServerName + userName );
    			_profileSuggestions.add(_connProfile.getKey() + userName);

    			// If profile name hasn't already been added, DO IT
    			// If it's a COPY, put the copy entry FIRST
				if ( StringUtil.hasValue(_connProfile.getName()) && ! _profileSuggestions.contains(_connProfile.getName()) )
				{
					if (_connProfile.getName().endsWith("_copy"))
						_profileSuggestions.add(0, _connProfile.getName());
					else
						_profileSuggestions.add(_connProfile.getName() + userName);
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
			_interfacesQ1_rbt.addActionListener(this); // call validateCompenents()
			_interfacesQ2_rbt.addActionListener(this); // call validateCompenents()
			_interfacesQ3_rbt.addActionListener(this); // call validateCompenents()
			
			ButtonGroup profileButGroup = new ButtonGroup();
			profileButGroup.add(_profileNameQ1_rbt);
			profileButGroup.add(_profileNameQ2_rbt);
			profileButGroup.add(_profileNameQ3_rbt);
			_profileNameQ1_rbt.addActionListener(this); // call validateCompenents()
			_profileNameQ2_rbt.addActionListener(this); // call validateCompenents()
			_profileNameQ3_rbt.addActionListener(this); // call validateCompenents()
			
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

				if (bussy && _interfacesQ1_rbt.isSelected() )
					enableOk = false;
			}

			if (_profileName_pan.isVisible())
			{
				String currentItem = _profileName_cbx.getEditor().getItem()+"";
				boolean bussy = getProfile(currentItem) != null;
				_profileName_bussy.setVisible(bussy);

				if (bussy && _profileNameQ1_rbt.isSelected())
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
						String prevName = _originConnProfile == null ? null : _originConnProfile.getName();
						
						String entry = StringUtil.getSelectedItemString(_profileName_cbx);
						_connProfile.setName(entry);

						// get current selected TreeNode (so we can add the new profile in the correct directory)
						DefaultMutableTreeNode catalogNode = getCatalogTreeNodeForEntry(prevName);
						DefaultMutableTreeNode profileNode = getProfileTreeNodeForEntry(prevName);

						// Add the profile (in catalog)
						addProfile(_connProfile, true, catalogNode, profileNode);
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
							conf.setProperty(PROPKEY_connProfile_serverAdd_showDialog, !DEFAULT_connProfile_serverAdd_showDialog);
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
							// Copy the file to $HOME/.dbxtune/sql.ini and add it there???
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
			
			validateCompenents();
		}
		
		public void showPossibly()
		{
			if (_interfacesName_pan.isVisible() || _profileName_pan.isVisible())
				setVisible(true);
			else
			{
				_logger.info("Save Dialog will NOT be opened... _interfacesName_pan.isVisible()="+_interfacesName_pan.isVisible()+", _profileName_pan.isVisible()="+_profileName_pan.isVisible());
			}
		}
	}

	private boolean saveChangesDialog(String key, ConnectionProfile connProfile, ConnProfileEntry newConnProfileEntry, String dbServerName, boolean afterSuccessfulConnect, JDialog owner, boolean showProfileOverride)
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

    		SaveAsDialog saveAs = new SaveAsDialog(owner, dbServerName, connProfile, connProfileCopy, afterSuccessfulConnect, showProfileOverride);
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
//					// Copy the file to $HOME/.dbxtune/sql.ini and add it there???
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
		private String _filterProfileName = null;
		
		public void   setFilterOnProductName(String name) { _filterProductName = name; }
		public String getFilterOnProductName()            { return _filterProductName; }

		public void   setFilterOnProfileName(String name) { _filterProfileName = name; }
		public String getFilterOnProfileName()            { return _filterProfileName; }

		private boolean filterInclude(DefaultMutableTreeNode node)
		{
			boolean show = true;
			Object o = node.getUserObject();
			if (o instanceof ConnectionProfile) 
			{
				ConnectionProfile connProfile = (ConnectionProfile) o;
//System.out.println("ConnectionProfileManager:FilteredTreeModel:filterInclude(): connProfile.getType()="+connProfile.getType()+", connProfile.getSrvType()="+connProfile.getSrvType()+", connProfile="+connProfile+", _filterProductName='"+_filterProductName+"'.");
	
				// Hide Server types of NOT desired products
				if (_filterProductName != null)
				{
					if ( ! connProfile.isSrvType(_filterProductName) )
						show = false;
				}

				// Hide Server types of NOT desired products
				if (StringUtil.hasValue(_filterProfileName))
				{
//					String profileName = connProfile.getName();
//					if ( profileName.indexOf(_filterProfileName) < 0 )
//						show = false;
					
					String profileName = connProfile.getName();
					
					try
					{
						Pattern pattern = Pattern.compile(_filterProfileName);
						Matcher matcher = pattern.matcher(profileName);
						if ( ! matcher.find() )
							show = false;
					}
					catch(PatternSyntaxException ex)
					{
						SwingUtils.showErrorMessage(null, "Faulty Regex", 
								  "<html>"
								+ "The regex '<b>"+_filterProfileName+"</b>' is not valid.<br>"
								+ "Error:"
								+ "<pre>"
								+ StringUtil.toHtmlString(ex.getMessage())
								+ "</pre>"
								+ "</html>", ex);
						throw ex;
					}
				}
			}
			return show;
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
		
		public static DefaultMutableTreeNode findTreeNode(DefaultMutableTreeNode root, String str)
		{
			@SuppressWarnings("unchecked")
//			Enumeration<DefaultMutableTreeNode> e = root.depthFirstEnumeration();
			Enumeration<TreeNode> e = root.depthFirstEnumeration();
			while (e.hasMoreElements())
			{
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
//				if ( node.toString().equalsIgnoreCase(str) )
				if ( node.toString().equals(str) )
				{
					return node;
				}
			}
			return null;
		}

		@SuppressWarnings("unused")
		public static TreePath findTreePath(DefaultMutableTreeNode root, String str)
		{
			@SuppressWarnings("unchecked")
//			Enumeration<DefaultMutableTreeNode> e = root.depthFirstEnumeration();
			Enumeration<TreeNode> e = root.depthFirstEnumeration();
			while (e.hasMoreElements())
			{
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
//				if ( node.toString().equalsIgnoreCase(str) )
				if ( node.toString().equals(str) )
				{
					return new TreePath(node.getPath());
				}
			}
			return null;
		}
	}

	public DefaultMutableTreeNode getCatalogTreeNodeForEntry(String profileName)
	{
		if (profileName == null)
			return null;
		
		DefaultMutableTreeNode profileNode = FilteredTreeModel.findTreeNode(_profileTreeRoot, profileName);
		if (profileNode == null)
			return null;
		
		DefaultMutableTreeNode parentTreeNode = (DefaultMutableTreeNode) profileNode.getParent();
		Object parentObj = parentTreeNode.getUserObject();
		if (parentObj instanceof ConnectionProfileCatalog)
		{
			return parentTreeNode;
		}
		return null;
	}

	public DefaultMutableTreeNode getProfileTreeNodeForEntry(String profileName)
	{
		if (profileName == null)
			return null;
		
		DefaultMutableTreeNode profileNode = FilteredTreeModel.findTreeNode(_profileTreeRoot, profileName);
		if (profileNode == null)
			return null;
		
		Object userObj = profileNode.getUserObject();
		if (userObj instanceof ConnectionProfile)
		{
			return profileNode;
		}
		return null;
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

	/** Increment this on every save */
	private int _saveCount = 0;
	/** true if a save is done by a background thread, and while the thread is active, other save requests will be queued... */
	private boolean _hasActiveSaveThread = false;

	/** internal save wrapper */
	private void save(final String filename, final boolean writeTemplateFile)
	{
		_saveCount++;
//System.out.println("ConnectionProfileManager.save(filename='"+filename+"', writeTemplateFile="+writeTemplateFile+") _saveCount="+_saveCount+".");

		boolean doBackgroundSave = true;
		if ( ! doBackgroundSave )
		{
			// DO THE SAVE
			saveInternal(filename, writeTemplateFile);
		}
		else
		{
			final int currentSaveCount = _saveCount;
			Runnable saveJob = new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						while (_hasActiveSaveThread)
						{
//System.out.println("ConnectionProfileManager.save() currentSaveCount="+currentSaveCount+". ------ WARNING: Waiting for previous thread to complete save...");
							Thread.sleep(250);
						}

						_hasActiveSaveThread = true;
						long startTime = System.currentTimeMillis();

						// DO THE SAVE
						saveInternal(filename, writeTemplateFile);
						
						long saveTime = System.currentTimeMillis() - startTime;
						if (saveTime > 1000)
							_logger.warn("ConnectionProfileManager.save() took "+saveTime+" ms... File name ='"+filename+"'. You might have a slow IO subsystem...");
//System.out.println("Configuration.save() currentSaveCount="+currentSaveCount+". TIME = "+saveTime+ (saveTime < 1000 ? "" : " ------- WARNING ------ WARNING ----- WARNING ---- SAVE Took to long time..."));
					}
					catch (Exception e)
					{
						_logger.error("Problems saving Connection Profile to the file='"+filename+"', currentSaveCount="+currentSaveCount+". Caught: "+e, e);
					}
					finally 
					{
						_hasActiveSaveThread = false;
					}
				}
			};
			Thread saveThread = new Thread(saveJob, "SaveConnProfile-" + currentSaveCount);
			saveThread.start();
		}
	}

	/** 
	 * Try to lock the file, if it fails, sleep 100ms and retry again <br>
	 * Max retry is 50 (so max 5 seconds)
	 */
	private FileLock lockWithRetry(FileChannel channel, String filename)
	throws IOException
	{
		OverlappingFileLockException lastEx = null;
		
		for (int i=0; i<50; i++) // retry 50 times... sleep 100ms SO MAX: 5 seconds
		{
			try
			{
				return channel.lock();
			}
			catch (OverlappingFileLockException ex)
			{
				lastEx = ex;
				
				_logger.info("Locking file issue. RetryCount=" + i + ". Sleeping 100ms and retry. File='" + filename + "', Caught: " + ex);

				try { Thread.sleep(100); }
				catch(InterruptedException ignore) {}
			}
		}
		
		if (lastEx != null)
			throw lastEx;
		throw new OverlappingFileLockException();
	}
	
	private void saveInternal(String filename, boolean writeTemplateFile)
	{
		if (StringUtil.isNullOrBlank(filename))
			filename = getFilename();

		LinkedHashMap<String, Throwable> problemMap = new LinkedHashMap<String, Throwable>();
		String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss-SSS").format(new Date());
		String tmpRecoveryFilename = filename + "." + timestamp + ".recover";

		String backupFilename_1 = filename + ".1.bak";
		String backupFilename_2 = filename + ".2.bak";
		String backupFilename_3 = filename + ".3.bak";

		try
		{
			File f = new File(filename);
			if (f.exists())
			{
				_logger.debug("ConnectionProfileManager.save(): saving a recovery file to '"+tmpRecoveryFilename+"', which will be deleted if no problems where found.");
				FileUtils.copy(filename, tmpRecoveryFilename);

				// Save to a Backup file... (but only if the backup file is older than 1 hour)
				// also: have 3 backup files (rolling over last file every time, so 3 will only be kept)
				long backupThreshold = 3_600_000;
				File backupFile_1 = new File(backupFilename_1);
				File backupFile_2 = new File(backupFilename_2);
				File backupFile_3 = new File(backupFilename_3);
				boolean doBackup = false;

				if ( ! backupFile_1.exists() )
				{
					doBackup = true;
				}
				else if ( (System.currentTimeMillis() - backupFile_1.lastModified()) > backupThreshold )
				{
					doBackup = true;
				}
				
				if ( doBackup )
				{
					// Remove oldest file (file 3), and move the rest of the files... (3=delete; 2->3; 1->2; overwrite 1)
					if (backupFile_3.exists()) backupFile_3.delete();
					if (backupFile_2.exists()) backupFile_2.renameTo(backupFile_3);
					if (backupFile_1.exists()) backupFile_1.renameTo(backupFile_2);
					
					_logger.debug("ConnectionProfileManager.save(): saving a backup file to '"+backupFilename_1+"'.");
					FileUtils.copy(filename, backupFilename_1);
				}
			}

			RandomAccessFile raf = new RandomAccessFile(filename, "rw");
			FileChannel channel = raf.getChannel();

			try 
			{
				// Get an exclusive lock on the whole file
//				FileLock lock = channel.lock();
				FileLock lock = lockWithRetry(channel, filename);

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

					// Write a "save date"
					sb.append("    ").append("<LastSaveDate>").append(TimeUtils.toString(System.currentTimeMillis())).append("</LastSaveDate>\n\n");

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
						
						sb.append("    ").append("<").append(XML_PROFILE_TREE_CATALOG).append(" ").append(XML_PROFILE_TREE_CATALOG_ATTR_NAME).append("=\"").append("Production") .append("\" ").append(XML_PROFILE_TREE_CATALOG_ATTR_EXPANDED).append("=\"").append(true).append("\">\n");
						sb.append("    ").append("</").append(XML_PROFILE_TREE_CATALOG).append(">\n");
						sb.append("\n");
						sb.append("    ").append("<").append(XML_PROFILE_TREE_CATALOG).append(" ").append(XML_PROFILE_TREE_CATALOG_ATTR_NAME).append("=\"").append("Development").append("\" ").append(XML_PROFILE_TREE_CATALOG_ATTR_EXPANDED).append("=\"").append(true).append("\">\n");
						sb.append("    ").append("</").append(XML_PROFILE_TREE_CATALOG).append(">\n");
						sb.append("\n");
						sb.append("    ").append("<").append(XML_PROFILE_TREE_CATALOG).append(" ").append(XML_PROFILE_TREE_CATALOG_ATTR_NAME).append("=\"").append("Test")       .append("\" ").append(XML_PROFILE_TREE_CATALOG_ATTR_EXPANDED).append("=\"").append(true).append("\">\n");
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
					// Write 'ProfileTypes' Structure
					if (writeTemplateFile)
					{
						// If we want to write a DUMMY Connection entry, this is the place to do it.
					}
					else
					{
						sb.setLength(0);

						sb.append("    <").append(XML_PROFILE_TYPES).append(">\n");
						sb.append("\n");
						
						for (ProfileType entry : getProfileTypes().values())
						{
							sb.append(entry.toXml());
							sb.append("\n");
						}

						sb.append("    </").append(XML_PROFILE_TYPES).append(">\n");
						sb.append("\n");

						byteBuffer = ByteBuffer.wrap(sb.toString().getBytes(Charset.forName("UTF-8")));
						channel.write(byteBuffer);
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
				String name           = ((ConnectionProfileCatalog)o).getName();
				String expandedMapStr = ((ConnectionProfileCatalog)o).getExpandedMapStr();
				
				sb.append(indentStr).append("<").append(XML_PROFILE_TREE_CATALOG).append(" ").append(XML_PROFILE_TREE_CATALOG_ATTR_NAME).append("=\"").append(name).append("\" ").append(XML_PROFILE_TREE_CATALOG_ATTR_EXPANDED).append("=\"").append(expandedMapStr).append("\">\n");
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
					try
					{
						ConnectionProfile entry = TdsEntry.parseXml((Element) node);
						addProfile(entry, false, (String)null);
					}
					catch (Throwable tr)
					{
						_logger.warn("Problems parsing an entry. This entry will be skipped. Entry='"+node.getTextContent()+"', File '"+filename+"'.", tr);
					}
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
					try
					{
						ConnectionProfile entry = JdbcEntry.parseXml((Element) node);
						addProfile(entry, false, (String)null);
					}
					catch (Throwable tr)
					{
						_logger.warn("Problems parsing an entry. This entry will be skipped. Entry='"+node.getTextContent()+"', File '"+filename+"'.", tr);
					}
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
					try
					{
						ConnectionProfile entry = OfflineEntry.parseXml((Element) node);
						addProfile(entry, false, (String)null);
					}
					catch (Throwable tr)
					{
						_logger.warn("Problems parsing an entry. This entry will be skipped. Entry='"+node.getTextContent()+"', File '"+filename+"'.", tr);
					}
		 		}
			}

			//----------------- ProfileTypes
			subTag = XML_PROFILE_TYPES;
			if (doc.getElementsByTagName(subTag).getLength() > 0)
			{
    			nlist  = doc.getElementsByTagName(subTag).item(0).getChildNodes();
    			if (nlist.getLength() > 0)
    			{
    				addToProfileTypes(nlist);
    			}
			}
//THIS_DOESNT_WORK_to_100_PERCENT

//			nlist  = doc.getElementsByTagName(subTag);
//			for (int i=0; i<nlist.getLength(); i++) 
//			{
//				Node node = nlist.item(i);
//		 
//				_logger.debug("Current Element at '"+subTag+"' :" + node.getNodeName());
//				if (node.getNodeType() == Node.ELEMENT_NODE) 
//				{
//					try
//					{
//						ProfileType entry = ProfileType.parseXml((Element) node);
//						addProfileType(entry);
//					}
//					catch (Throwable tr)
//					{
//						_logger.warn("Problems parsing an entry. This entry will be skipped. Entry='"+node.getTextContent()+"', File '"+filename+"'.", tr);
//					}
//		 		}
//			}
			if (_profileTypesMap.size() == 0)
			{
				createDefaultProfileTypes();
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
				_profileTreeRoot.add(new DefaultMutableTreeNode(new ConnectionProfileCatalog("Production",  true), true));
				_profileTreeRoot.add(new DefaultMutableTreeNode(new ConnectionProfileCatalog("Development", true), true));
				_profileTreeRoot.add(new DefaultMutableTreeNode(new ConnectionProfileCatalog("Test",        true), true));
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


	private void addToProfileTypes(NodeList nlist)
	{
		for (int i=0; i<nlist.getLength(); i++) 
		{
			Node node = nlist.item(i);
	 
			if (node.getNodeType() == Node.ELEMENT_NODE) 
			{
				String tagName = node.getNodeName();

				if (XML_PROFILE_TYPE_ENTRY.equals(tagName))
				{
					try
					{
						ProfileType entry = ProfileType.parseXml((Element) node);
						addProfileType(entry);
					}
					catch (Throwable tr)
					{
						_logger.warn("Problems parsing an entry. This entry will be skipped. Entry='"+node.getTextContent()+"', File '"+getFilename()+"'.", tr);
					}
				}
				else
				{
					_logger.warn("Connection Profile found unknwon XML tag '"+tagName+"' when parsing file '"+getFilename()+"'. Skipping this and continuing with next one...");
				}
	 		}
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
					String name        = ((Element)node).getAttribute(XML_PROFILE_TREE_CATALOG_ATTR_NAME);
					String expandedStr = ((Element)node).getAttribute(XML_PROFILE_TREE_CATALOG_ATTR_EXPANDED);

					DefaultMutableTreeNode newTreeNode = new DefaultMutableTreeNode(new ConnectionProfileCatalog(name, expandedStr), true);
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

	/**
	 * Set the color and size of the main border around the window
	 * @param profileType
	 */
	public static void setBorderForConnectionProfileType(Container cont, String profileTypeName)
	{
		setBorderForConnectionProfileType( cont, getInstance().getProfileTypeByName(profileTypeName) );
	}
	public static void setBorderForConnectionProfileType(Container cont, ProfileType profileType)
	{
		if ( ! (cont instanceof JPanel) )
		{
			_logger.debug("setConnectionProfileType(): returning without doing anything. Passed Container is not JPanel. cont="+cont);
			return;
		}
		JPanel contentPane = (JPanel) cont;

		// Set empty border
		if (profileType == null)
		{
			_logger.debug("Setting Connection Profile to emptyBorder, since a 'null' profileType was passed.");
			contentPane.setBorder(BorderFactory.createEmptyBorder());
			return;
		}

		if (_logger.isDebugEnabled())
			_logger.debug("Setting Connection Profile Type to name='"+profileType.getName()+"' in Container=."+cont);

		if (    profileType._borderMargins.top    == 0 
		     && profileType._borderMargins.left   == 0 
		     && profileType._borderMargins.bottom == 0 
		     && profileType._borderMargins.right  == 0 
		   )
		{
			contentPane.setBorder(BorderFactory.createEmptyBorder());
			return;
		}

		contentPane.setBorder(BorderFactory.createMatteBorder(
				SwingUtils.hiDpiScale(profileType._borderMargins.top), 
				SwingUtils.hiDpiScale(profileType._borderMargins.left), 
				SwingUtils.hiDpiScale(profileType._borderMargins.bottom), 
				SwingUtils.hiDpiScale(profileType._borderMargins.right), 
				profileType._color));
	}
}
